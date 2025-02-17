package me.kryniowesegryderiusz.kgenerators.generators.holograms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;

import lombok.Getter;
import me.kryniowesegryderiusz.kgenerators.Main;
import me.kryniowesegryderiusz.kgenerators.addons.events.HologramReplaceLinesEvent;
import me.kryniowesegryderiusz.kgenerators.dependencies.enums.Dependency;
import me.kryniowesegryderiusz.kgenerators.dependencies.objects.CMIHologramsProvider;
import me.kryniowesegryderiusz.kgenerators.dependencies.objects.DecentHologramsProvider;
import me.kryniowesegryderiusz.kgenerators.dependencies.objects.HolographicDisplaysProvider;
import me.kryniowesegryderiusz.kgenerators.generators.generator.objects.Generator;
import me.kryniowesegryderiusz.kgenerators.generators.holograms.interfaces.IHologramProvider;
import me.kryniowesegryderiusz.kgenerators.generators.locations.objects.GeneratorLocation;
import me.kryniowesegryderiusz.kgenerators.lang.Lang;
import me.kryniowesegryderiusz.kgenerators.lang.enums.HologramText;
import me.kryniowesegryderiusz.kgenerators.logger.Logger;

public class HologramsManager {

	@Getter
	private IHologramProvider hologramProvider;

	private ArrayList<GeneratorLocation> holograms = new ArrayList<>();

	public HologramsManager() {

		Logger.debugPluginLoad("HologramsManager: Setting up manager");

		if (Main.getDependencies().isEnabled(Dependency.DECENT_HOLOGRAMS)) {
			hologramProvider = new DecentHologramsProvider();
			Logger.debugPluginLoad("Holograms: Enabling DecentHologramsProvider");
		} else if (Main.getDependencies().isEnabled(Dependency.HOLOGRAPHIC_DISPLAYS)) {
			hologramProvider = new HolographicDisplaysProvider();
			Logger.debugPluginLoad("Holograms: Enabling HolographicDisplaysProvider");
		} else if (Main.getDependencies().isEnabled(Dependency.CMI_HOLOGRAMS)) {
			hologramProvider = new CMIHologramsProvider();
			Logger.debugPluginLoad("Holograms: Enabling CMIHologramsProvider");
		} else {
			for (Map.Entry<String, Generator> e : Main.getGenerators().getEntrySet()) {
				if ((e.getValue()).isHologram())
					Logger.warn("Holograms: Generator " + e.getKey()
							+ " has enabled holograms, but hologram provider was not found! Holograms wont work!");
			}
		}

		Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getInstance(), new Runnable() {
			public void run() {
				if (hologramProvider == null)
					return;

				Iterator<GeneratorLocation> iter = holograms.iterator();
				while (iter.hasNext()) {
					GeneratorLocation gLocation = iter.next();

					if (Main.getPlacedGenerators().isLoaded(gLocation) && Main.getSchedules().timeLeft(gLocation) > 0) {
						hologramProvider.updateHologram(gLocation,
								Main.getHolograms().getHologramRemainingTimeLines(gLocation));
					} else {
						iter.remove();
					}
				}
			}
		}, 0L, Main.getSettings().getHologramUpdateFrequency() * 1L);
	}

	public void createRemainingTimeHologram(GeneratorLocation gLocation) {
		this.createHologram(gLocation, this.getHologramRemainingTimeLines(gLocation));
	}

	public void createHologram(GeneratorLocation gLocation, ArrayList<String> lines) {
		if (hologramProvider == null)
			return;
		if (gLocation == null)
			return;
		hologramProvider.createHologram(gLocation, lines);
		if (!holograms.contains(gLocation))
			holograms.add(gLocation);
	}

	public void removeHologram(GeneratorLocation gLocation) {
		if (hologramProvider == null)
			return;
		hologramProvider.removeHologram(gLocation);
		if (holograms.contains(gLocation))
			holograms.remove(gLocation);
	}

	public ArrayList<String> getHologramRemainingTimeLines(GeneratorLocation gLocation) {

		HologramReplaceLinesEvent e = new HologramReplaceLinesEvent(gLocation, HologramText.REMAINING_TIME);

		Main.getInstance().getServer().getPluginManager().callEvent(e);

		ArrayList<String> lines = new ArrayList<>();
		String time = Main.getSchedules().timeLeftFormatted(gLocation);
		for (String s : Lang.getHologramTextStorage().get(HologramText.REMAINING_TIME).getLines()) {
			if (s.contains("<time>"))
				s = s.replaceAll("<time>", time);
			if (s.contains("<generator_name>"))
				s = s.replaceAll("<generator_name>", gLocation.getGenerator().getGeneratorItemName());

			for (Entry<String, String> en : e.getReplacablesMap().entrySet()) {
				s = s.replaceAll(en.getKey(), en.getValue());
			}

			lines.add(s);
		}
		return lines;
	}
}
