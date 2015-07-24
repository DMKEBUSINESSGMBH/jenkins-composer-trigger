package de.dmkebusiness.jenkins.composertrigger;

import hudson.Launcher;
import hudson.model.TaskListener;

import java.io.ByteArrayOutputStream;

import org.apache.commons.lang.StringUtils;

public class Main {

	public static void main(String[] args) throws Exception {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		new Launcher.LocalLauncher(TaskListener.NULL)
				.launch()
				.cmds("C:\\Entwicklung\\php\\xampp\\php\\php.exe",
						"C:\\ProgramData\\ComposerSetup\\bin\\composer.phar", "update", "--dry-run")
				.pwd("C:\\Entwicklung\\java\\workspace\\composer-trigger\\work\\jobs\\test-projekt-composertrigger\\workspace")
				.stdout(baos).join();

		System.out.println(baos.toString());
		System.out.println();
		System.out.println(StringUtils.indexOf(baos.toString(), "Nothing to install"));
	}
}