package com.nao20010128nao.PluginEliminator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.permission.Permission;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;

/**
 * Thank you to my redistributed work! (PlayerPrivacy)
 */
public class PluginMain extends PluginBase implements Listener {

	public PluginMain() {
		// TODO 自動生成されたコンストラクター・スタブ
	}

	@Override
	public void onEnable() {
		getServer().getCommandMap().register("delete", new CommandDispatcher("delete"));

	}

	@Override
	public void onDisable() {

	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!label.equalsIgnoreCase("delete"))
			return true;
		if (args.length != 1) {
			sender.sendMessage("Arguments error!");
			return true;
		}
		getServer().getPluginManager().getPlugins().values().stream()
				.filter(plg -> plg.getName().equalsIgnoreCase(args[0]))
				.forEach(plg -> {
					sender.sendMessage(TextFormat.YELLOW + "Trying to " + TextFormat.RED + TextFormat.BOLD + "delete"
							+ TextFormat.YELLOW + ": " + plg.getName());
					try {
						eliminate(plg);
						sender.sendMessage(TextFormat.GREEN + "Deleted. Bye!");
					} catch (Throwable e) {
						sender.sendMessage(TextFormat.RED + "Error.");
					}
				});
		return true;
	}

	/**
	 * Disables the plugin without calling plugin.onDisable() and messages.
	 * After that, delete the plugin file if we can.
	 */
	private void eliminate(Plugin plugin) throws Throwable {
		if (plugin.isEnabled()) {
			getServer().getScheduler().cancelTask(plugin);
			HandlerList.unregisterAll(plugin);
			for (Permission permission : plugin.getDescription().getPermissions())
				getServer().getPluginManager().removePermission(permission);
		}
		File where = findPath(plugin);
		ClassLoader cl = plugin.getClass().getClassLoader();
		if (cl instanceof URLClassLoader)
			((URLClassLoader) cl).close();
		if (where != null)
			if (where.isDirectory())
				deleteRescursive(where);
			else if (where.isFile()) {
				if (!where.delete())
					where.deleteOnExit();
				startDeletionDaemon(where);
			}
	}

	/**
	 * Try to get where the plugin exists.
	 * We'll try to get its path as much as we can.
	 */
	private File findPath(Plugin p) {
		// Try to get the object of the "file" field.
		try {
			Field field = PluginBase.class.getDeclaredField("file");
			field.setAccessible(true);
			return (File) field.get(p);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
		// If we can't, try to get the path from the ClassLoader
		// (if the ClassLoader is URLClassLoader or its desendant)
		ClassLoader cl = p.getClass().getClassLoader();
		if (cl instanceof URLClassLoader) {
			List<File> files = Arrays.stream(((URLClassLoader) cl).getURLs())
					.filter(url -> url.getProtocol().equalsIgnoreCase("file"))
					.map(url -> {
						try {
							return new File(url.toURI());
						} catch (Exception e) {
							return null;
						}
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			if (files.size() != 0)
				return files.get(0);
		}
		// We have no more way get its path, so we return null
		return null;
	}

	private void startDeletionDaemon(File f) {
		String javaLibraryPath = System.getProperty("java.library.path");
		File javaExeFile = new File(javaLibraryPath.substring(0, javaLibraryPath.indexOf(';')));
		String javaExePath = null;
		for (String s : new String[] { "java", "java.exe" })
			if (new File(javaExeFile, s).exists())
				javaExePath = new File(javaExeFile, s).getAbsolutePath();
		if (javaExePath == null)
			javaExePath = "java";
		try {
			new ProcessBuilder(javaExePath, "-classpath", findPath(this).getAbsolutePath(), Daemon.class.getName(),
					f.getAbsolutePath()).start();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	private void deleteRescursive(File f) {
		if (f.isDirectory()) {
			for (File cf : f.listFiles())
				deleteRescursive(cf);
			f.delete();
		} else if (f.isFile())
			f.delete();
	}

	class CommandDispatcher extends Command {

		public CommandDispatcher(String name) {
			super(name);
		}

		@Override
		public boolean execute(CommandSender sender, String commandLabel, String[] args) {
			return onCommand(sender, this, commandLabel, args);
		}
	}
}
