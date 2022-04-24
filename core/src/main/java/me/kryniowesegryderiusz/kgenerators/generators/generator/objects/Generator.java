package me.kryniowesegryderiusz.kgenerators.generators.generator.objects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import lombok.Getter;
import me.kryniowesegryderiusz.kgenerators.Main;
import me.kryniowesegryderiusz.kgenerators.api.objects.AbstractGeneratedObject;
import me.kryniowesegryderiusz.kgenerators.generators.generator.GeneratorsManager;
import me.kryniowesegryderiusz.kgenerators.generators.generator.enums.GeneratorType;
import me.kryniowesegryderiusz.kgenerators.generators.upgrades.objects.Upgrade;
import me.kryniowesegryderiusz.kgenerators.logger.Logger;
import me.kryniowesegryderiusz.kgenerators.settings.objects.Actions;
import me.kryniowesegryderiusz.kgenerators.utils.immutable.Config;
import me.kryniowesegryderiusz.kgenerators.utils.immutable.RandomSelector;
import me.kryniowesegryderiusz.kgenerators.xseries.XMaterial;
import me.kryniowesegryderiusz.kgenerators.xseries.XUtils;

public class Generator {
	
	@Getter private String id;
	private ItemStack generatorItem;
	@Getter private int delay;
	@Getter private GeneratorType type = GeneratorType.SINGLE;
    private LinkedHashMap<AbstractGeneratedObject, Double> chances = new LinkedHashMap<AbstractGeneratedObject, Double>();
    
	@Getter private ItemStack placeholder;
	@Getter private boolean generateImmediatelyAfterPlace;
	@Getter private boolean allowPistonPush = false;
	@Getter private boolean hologram = false;
    @Getter private Actions actions = null;
    @Getter private ArrayList<String> disabledWorlds = new ArrayList<String>();
    
    @Getter private boolean secondMultiple = false;
    private double fullChance = 0.0;
    @Getter boolean loaded = false;
	
	@SuppressWarnings("unchecked")
	public Generator(GeneratorsManager generatorsManager, Config config, String generatorID) {

		Boolean error = false;
		
		/*
		 * Required options
		 */
		
		this.id = generatorID;
		
		this.delay = config.getInt(generatorID+".delay");
	    double sm = (double) delay/20;
	    if (sm == Math.floor(sm)) secondMultiple = true;
	    
		this.type = GeneratorType.getGeneratorTypeByString(config.getString(generatorID+".type"));
		
		Boolean glow = true;
		if (config.contains(generatorID+".glow")) {
			glow = config.getBoolean(generatorID+".glow");
		}
		this.generatorItem = XUtils.parseItemStack(config.getString(generatorID+".generator"), "Generators file", true);
		ArrayList<String> loreGot = new ArrayList<String>();
		ArrayList<String> lore = new ArrayList<String>();
		ItemMeta meta = (ItemMeta) this.generatorItem.getItemMeta();
		meta.setDisplayName(Main.getMultiVersion().getChatUtils().colorize(config.getString(generatorID+".name")));
		loreGot = (ArrayList<String>) config.getList(generatorID+".lore");
	    for (String l : loreGot) {
	    	l = Main.getMultiVersion().getChatUtils().colorize(l);
	    	lore.add(l);
	      }
		meta.setLore(lore);
		lore.clear();
		if (glow)
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		this.generatorItem.setItemMeta(meta);
		if (glow)
			this.generatorItem.addUnsafeEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 1);
		
		if (config.contains(generatorID+".generates"))
		    for (Map<?, ?> generatedObjectConfig : (List<Map<?, ?>>) config.getMapList(generatorID+".generates")) {
	
		    	AbstractGeneratedObject ago = generatorsManager.getGeneratedObjectsManager().getNewObject((String) generatedObjectConfig.get("type"));
		    	if (ago != null && ago.load(generatedObjectConfig))
	    			this.chances.put(ago, ago.getChance());
		    }
		else
			Logger.error("Generators file: " + generatorID + " doesnt set any generating objects set!");
		
		/*
		 * Optional options
		 */
		
		if (config.contains(generatorID+".placeholder") && !config.getString(generatorID+".placeholder").isEmpty()) {
			this.placeholder = XUtils.parseItemStack(config.getString(generatorID+".placeholder"), "Generators file", true);
			if (this.placeholder.equals(XMaterial.AIR.parseItem()))
				this.placeholder = null;
		}
		
		if (config.contains(generatorID+".generate-immediately-after-place") && !config.getString(generatorID+".generate-immediately-after-place").isEmpty()) {
			this.generateImmediatelyAfterPlace = config.getBoolean(generatorID+".generate-immediately-after-place");
		}
		
		if (config.contains(generatorID+".allow-piston-push")) {
			this.allowPistonPush = config.getBoolean(generatorID+".allow-piston-push");
		}
		
		if (config.contains(generatorID+".hologram")) {
			this.hologram = config.getBoolean(generatorID+".hologram");
		}
		
		if (config.contains(generatorID+".disabled-worlds")) {
			this.getDisabledWorlds().addAll((ArrayList<String>) config.getList(generatorID+".disabled-worlds"));
		}
		
		if (config.contains(generatorID+".actions")) {
			this.actions = new Actions();
			this.actions.load(config, generatorID);
		}
		
		String doubledGeneratorId = generatorsManager.exactGeneratorItemExists(generatorID, this.getGeneratorItem());
		if (doubledGeneratorId != null) {
			Logger.error("Generators file: " + generatorID + " has same generator item as " + doubledGeneratorId);
			error = true;
		}
		
		if (generatorsManager.exists(generatorID)) {
			Logger.error("Generators file: generatorID of " + generatorID + "is duplicated!");
			error = true;
		}
		
		if (error) {
			Logger.error("Generators file: Couldnt load " + generatorID);
		} else {
			generatorsManager.add(generatorID, this);        		
    		Logger.debug("Generators file: Loaded " + type + " "+ generatorID + " generating variety of " + this.chances.size() + " objects every " + delay + " ticks");
		}
	}
	
	public boolean doesChancesContain(AbstractGeneratedObject generatedObject)
	{
		for (Entry<AbstractGeneratedObject, Double> e : chances.entrySet())
		{
			if (e.getKey().equals(generatedObject)) return true;
		}
		return false;
	}
	
	public double getChancePercent(AbstractGeneratedObject ago)
	{
		double chance = chances.get(ago);
		double fullchance = this.fullChance;
		double percent = (chance/fullchance) * 100;
		return percent;
	}
	
	public String getChancePercentFormatted(AbstractGeneratedObject ago)
	{
		double percent = this.getChancePercent(ago);
		return String.format("%.2f", percent);
	}
	
	public Set<Entry<AbstractGeneratedObject, Double>> getChancesEntryset() {
		return this.chances.entrySet();
	}
	
	public AbstractGeneratedObject drawGeneratedObject() {
		RandomSelector<AbstractGeneratedObject> selector = RandomSelector.weighted(this.chances.keySet(), s -> this.chances.get(s));
		return selector.next(new Random());
	}
	
	public ItemStack getGeneratorItem()
	{
		return this.generatorItem.clone();
	}
	
	public String getGeneratorItemName()
	{
		return this.generatorItem.getItemMeta().getDisplayName();
	}
	
	public Upgrade getUpgrade()
	{
		return Main.getUpgrades().getUpgrade(id);
	}
	
	public boolean isWorldDisabled(World w)
	{
		return Main.getSettings().isWorldDisabled(w) || this.disabledWorlds.contains(w.getName());
	}
}
