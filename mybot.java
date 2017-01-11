package com.mycompany.mybot;

import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

import cz.cuni.amis.introspection.java.JProp;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathExecutorState;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.utils.Pogamut;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.TabooSet;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.UT2004PathAutoFixer;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.stuckdetector.UT2004DistanceStuckDetector;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.stuckdetector.UT2004PositionStuckDetector;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.stuckdetector.UT2004TimeStuckDetector;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Move;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Rotate;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Stop;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.StopShooting;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Jump;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.NavPoint;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerDamaged;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerKilled;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.exception.PogamutException;
import cz.cuni.amis.utils.flag.FlagListener;
/**
 * Author: Steven Impson
 * using portions of code and template from HunterBot (pogamut.cuni.cz/pogamut_files/latest/doc/tutorials/)
 */
@AgentScoped
public class EmptyBot extends UT2004BotModuleController<UT2004Bot> {

    @JProp
    public String stringProp = "Hello bot example";
    @JProp
    public boolean boolProp = true;
    @JProp
    public int intProp = 2;
    @JProp
    public double doubleProp = 1.0;
    @JProp
    public boolean findHealth = true;
    //boolean switch to pursue health
    @JProp
    public boolean pursueEnemy = true;
    //switch to pursue enemy player
    @JProp
    public boolean shouldEngage = true;
    //switch to determine whether bot will engage an enemy on sight
    @JProp
    public boolean findItems = true;
    //switch to find item behaviour
    @JProp
    public int healthThreshold = 50;
    //how low health must get to trigger findHealth behaviour
    
    protected Player enemy = null;
    //used to track info about the enemy currently being hunted
           
    protected TabooSet<Item> tabooItems = null;
    //holds a set of items that the bot will not pursue when in findItems behaviour
    
    private UT2004PathAutoFixer autoFixer;

    /**
     * Initialize all necessary variables here, before the bot actually receives anything
     * from the environment.
     */
    @Override
    public void prepareBot(UT2004Bot bot) {
        
        
        // stuck detector detects if bot has not moved for three seconds, or stays within a set position range (using position history)
        //stuck detector uses code from Hunterbot
        pathExecutor.addStuckDetector(new UT2004TimeStuckDetector(bot, 3000, 10000)); 
        pathExecutor.addStuckDetector(new UT2004PositionStuckDetector(bot)); 
        pathExecutor.addStuckDetector(new UT2004DistanceStuckDetector(bot)); // watch over distances to target

        autoFixer = new UT2004PathAutoFixer(bot, pathExecutor, fwMap, aStar, navBuilder); // auto-removes wrong navigation links between navpoints
        
        // DEFINE WEAPON PREFERENCES
        weaponPrefs.addGeneralPref(UT2004ItemType.MINIGUN, false);
        weaponPrefs.addGeneralPref(UT2004ItemType.LIGHTNING_GUN, true);                
        weaponPrefs.addGeneralPref(UT2004ItemType.SHOCK_RIFLE, false);
        weaponPrefs.addGeneralPref(UT2004ItemType.FLAK_CANNON, true);        
        weaponPrefs.addGeneralPref(UT2004ItemType.ROCKET_LAUNCHER, false);
        weaponPrefs.addGeneralPref(UT2004ItemType.LINK_GUN, false);
        weaponPrefs.addGeneralPref(UT2004ItemType.ASSAULT_RIFLE, true);        
        weaponPrefs.addGeneralPref(UT2004ItemType.BIO_RIFLE, true);
        
        //initialises previousState for use when changing and checking state
        previousState = State.OTHER;
    }
    
    //initialises details of bot, name
    @Override
    public Initialize getInitializeCommand() {
        return new Initialize().setName("Herman").setDesiredSkill(4);
    }
    
    //list of available states
    protected static enum State {
        
        HEALTH,
        PURSUE,
        ENGAGE,
        ITEMS,
        GRAB,
        OTHER
       
    }
    
    //resets bot state to default, start logic state from scratch
    
    protected void reset(){
        previousState = State.OTHER;
        enemy = null;
        itemsToRunAround = null;
        notMoving = 0;
        item = null;
        navigation.stopNavigation();
        
    }
    
    //saves the previous state the bot was in in last logic iteration
    protected State previousState = State.OTHER;
    
    //variable measures whether bot has stopped moving
    protected int notMoving = 0;
    
    //reports and logs when bot damages another player
    @EventListener(eventClass=PlayerDamaged.class)
    public void playerDamaged(PlayerDamaged event) {
    	log.info("Bot just hurt: " + event.getDamage());
    }
    
    //reports and logs when bot is damaged by another player
    @EventListener(eventClass=BotDamaged.class)
    public void botDamaged(BotDamaged event) 
    {
        log.info("Bot hurt by: " + event.getDamage());
    }
    
    
   //logic is main method that will iterate and decide on current state
    @Override
    public void logic() {
        //stuck detection mechanism
        if(!info.isMoving()) {
            ++notMoving;
            if (notMoving > 5){
                //bot is stuck, reset the bot's logic
                reset();
                return;
            }
        }
        
        //can the the bot see an enemy, it should pursue them
        if (shouldEngage && players.canSeeEnemies() && weaponry.hasLoadedWeapon() ){
            stateEngage();
            return;
        }
        
        //is bot shooting? Stop shooting unless shooting state starts again
        if(info.isShooting() || info.isSecondaryShooting()) {
            getAct().act(new StopShooting());
        }
        
        //is bot being shot, go to hit state (bot turns to face shooter)
       if (senses.isBeingDamaged()) {
           this.stateHit();
           return;
        }
       
       //enemy appears to pursue, is allowed to pursue and have a loaded weapon
       if (enemy != null && pursueEnemy && weaponry.hasLoadedWeapon()){
           this.statePursue();
           return;
       }
       
       //if bot is hurt, seek health
       if(info.getHealth() < healthThreshold && canRunAlongMedKit()){
           this.stateMedKit();
           return;
       }
       
       //grab items if visible, chooses appropriate item
       
       if (findItems && !items.getVisibleItems().isEmpty()) {
           item = getNearestVisibleItem();
           if (item != null && fwMap.getDistance(info.getNearestNavPoint(), item.getNavPoint()) <500) {
               stateSeeItem();
                previousState = State.GRAB;
                return;
           }
           
       }
       
       //otherwise, walk around finding items until one of the other states is triggered
       stateRunAroundItems();
       
    }
    
    //ENGAGE state
    
    protected boolean runningToPlayer = false; 
    
    //this state is fired when the bot sees an enemy (as above)
    
    protected void stateEngage() {
        log.info("Bot has decided to ENGAGE");
        
        boolean shooting = false;
        double distance = Double.MAX_VALUE;
        
        //check if enemy still in sight, pick a new one if not
        if (previousState != State.ENGAGE || enemy == null || !enemy.isVisible()){
            //picks a new enemy in sight
            enemy = players.getNearestVisiblePlayer(players.getVisibleEnemies().values());
            if(enemy == null) {
                log.info("No enemies in sight");
                return;
            }
            if(info.isShooting()){
                getAct().act(new Jump());
                //stops shooting
                getAct().act(new StopShooting());   
            }
            runningToPlayer = false;
    }
      
    if(enemy != null) {
        //if not already shooting at enemy, shoot it
        distance = info.getLocation().getDistance(enemy.getLocation());
        
        if(shoot.shoot(weaponPrefs, enemy) != null) {
            log.info("Bot is shooting!");
            shooting = true;
        } 
    }
    
    }
    
    //if hit, bot enters hit state and turns around until it sees shooter, at which time another state will be invoked
    protected void stateHit() {
        log.info("Bot state is HIT");
        getAct().act(new Rotate().setAmount(32000));
        previousState = State.OTHER;
    }
    
    //state where bot will pursue an enemy that has gone out of sight
    //bot will pursue enemy for 30 logic cycles. if it reaches 30 cycles it will give up and 
    //go back to default behaviour
    //based on example from HunterBot
    protected void statePursue(){
        log.info("Bot is pursuing an enemy");
        if(previousState != State.PURSUE){
            pursueCount = 0;
            navigation.navigate(enemy);
        }
        ++pursueCount;
        if(pursueCount > 30){
            reset();
        }
        else{
            previousState = State.PURSUE;
        }
    }
    protected int pursueCount = 0;
    
    //state where bot will pursue health
    protected void stateMedKit(){
        log.info("Bot is low on health, pursuing medkit");
        if(previousState != State.HEALTH){
            List<Item> healthKits = new LinkedList();
            healthKits.addAll(items.getSpawnedItems(UT2004ItemType.HEALTH_PACK).values());
            if(healthKits.size() == 0){
                healthKits.addAll(items.getSpawnedItems(UT2004ItemType.MINI_HEALTH_PACK).values());
            }
            if(healthKits.size() == 0){
                log.info("No health to run to");
                stateRunAroundItems();
                return;
            }
            item = fwMap.getNearestItem(healthKits, info.getNearestNavPoint());
            navigation.navigate(item);
        }
        previousState = State.HEALTH;
    }
    
    //state called when bot sees an item
    protected Item item = null;
    
    protected void stateSeeItem() {
        log.info("Bot has seen an item");
        //checks if item has been seen, and whether it is within easy reach. if not, reset mind
        if(item != null && item.getLocation().getDistance(info.getLocation()) < 80){
            reset();
        }
        //if item is within easy reach, bot navigates to it
        if(item != null && previousState != State.GRAB){
            if(item.getLocation().getDistance(info.getLocation()) < 300){
                getAct().act(new Move().setFirstLocation(item.getLocation()));
            }
            else{
                navigation.navigate(item);
            }    
        }
    }
    //code from HunterBot
    protected Item getNearestPossiblySpawnedItemOfType(ItemType type) {
    	final NavPoint nearestNavPoint = info.getNearestNavPoint();
    	List<Item> itemsDistanceSortedAscending = 
    			DistanceUtils.getDistanceSorted(
    					items.getSpawnedItems(type).values(), 
    					info.getLocation(), 
    					new DistanceUtils.IGetDistance<Item>() {
							@Override
							public double getDistance(Item object, ILocated target) {
								return fwMap.getDistance(nearestNavPoint, object.getNavPoint());
							}
						}
    			);
    	if (itemsDistanceSortedAscending.size() == 0) return null;
    	return itemsDistanceSortedAscending.get(0);
    }
    //code from Hunterbot
    protected Item getNearestVisibleItem() {
    	final NavPoint nearestNavPoint = info.getNearestNavPoint();
    	List<Item> itemsDistanceSortedAscending = 
    			DistanceUtils.getDistanceSorted(
    					items.getVisibleItems().values(), 
    					info.getLocation(), 
    					new DistanceUtils.IGetDistance<Item>() {
							@Override
							public double getDistance(Item object, ILocated target) {
								return fwMap.getDistance(nearestNavPoint, object.getNavPoint());
							}
						}
    			);
    	if (itemsDistanceSortedAscending.size() == 0) return null;
    	return itemsDistanceSortedAscending.get(0);
    }
    //determines whether there is a medkit (large or small) on the map and returns boolean
    protected boolean canRunAlongMedKit(){
        boolean result = !items.getSpawnedItems(UT2004ItemType.HEALTH_PACK).isEmpty() || !items.getSpawnedItems(UT2004ItemType.MINI_HEALTH_PACK).isEmpty();
        return result;
    }
    
    //state in which bot has nothing better to do than run around in search of items
    
    protected List<Item> itemsToRunAround = null;
    
    protected void stateRunAroundItems(){
        log.info("Bot is searching for items");
        if(previousState != State.ITEMS){
            itemsToRunAround = new LinkedList<Item>(items.getSpawnedItems().values());
            if(itemsToRunAround.size() == 0){
            log.info("No items around");
            reset();
            return;
        }
            item = itemsToRunAround.iterator().next();
            navigation.navigate(item);
        }
        previousState = State.ITEMS;
    }
    
    //resets certain variables when bot is killed
    
    @Override
    public void botKilled(BotKilled event){
        itemsToRunAround = null;
        enemy = null;
    }
    
    /**
     * This method is called when the bot is started either from IDE or from command line.
     *
     * @param args
     */
    public static void main(String args[]) throws PogamutException {
    	// wrapped logic for bots executions, suitable to run single bot in single JVM
    	new UT2004BotRunner(EmptyBot.class, "EmptyBot").setMain(true).startAgent();
    }
}
