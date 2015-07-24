package de.dmkebusiness.jenkins.composertrigger;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildableItem;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.model.Cause;
import hudson.model.Node;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import antlr.ANTLRException;

public class ComposerTrigger extends Trigger<BuildableItem> {

	private static final Logger LOGGER = Logger.getLogger(ComposerTrigger.class.getName());

	@DataBoundConstructor
	public ComposerTrigger(String spec) throws ANTLRException {
		super(spec);
	}

	@Override
	public void run() {
		Node node = super.job.getLastBuiltOn();

		if (node == null) {
			LOGGER.info("no previous build found so skip update trigger");
			return;
		}

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			new Launcher.LocalLauncher(TaskListener.NULL, node.getChannel())
					.launch()
					.cmds("C:\\Entwicklung\\php\\xampp\\php\\php.exe",
							"C:\\ProgramData\\ComposerSetup\\bin\\composer.phar", "update", "--dry-run")
					.pwd(super.job.getRootDir()).stdout(baos).join();

			LOGGER.info("trigger? " + StringUtils.indexOf(baos.toString(), "Nothing to install") + " ("
					+ baos.toString() + ")");

			if (StringUtils.indexOf(baos.toString(), "Nothing to install") < 0) {
				LOGGER.info("-> yes");
				job.scheduleBuild(0, new Cause() {
					@Override
					public String getShortDescription() {
						return "dep has an update";
					}
				});
			} else {
				LOGGER.info("-> no");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Extension
	public static final class DescriptorImpl extends TriggerDescriptor {
		@Override
		public boolean isApplicable(Item item) {
			return item instanceof BuildableItem;
		}

		@Override
		public String getDisplayName() {
			return "ComposerTrigger";
		}
	}
}
