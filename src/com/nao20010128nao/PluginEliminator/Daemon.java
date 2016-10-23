package com.nao20010128nao.PluginEliminator;

import java.io.File;

public class Daemon {
	public static void main(String[] args) throws InterruptedException {
		File file = new File(args[0]);
		while (true) {
			file.delete();
			if (!file.exists())
				System.exit(0);
			Thread.sleep(1500);
		}
	}
}
