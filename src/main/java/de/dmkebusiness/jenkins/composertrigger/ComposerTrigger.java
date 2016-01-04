package de.dmkebusiness.jenkins.composertrigger;

import hudson.Extension;

import hudson.model.BuildableItem;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.model.Cause;
import hudson.model.Node;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.RobustReflectionConverter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import antlr.ANTLRException;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.mapper.Mapper;

public class ComposerTrigger extends Trigger<BuildableItem> {

	private static final Logger LOGGER = Logger.getLogger(ComposerTrigger.class.getName());

	private final String php;
	private final String composerPhar;
	private final String composerJson;

	@DataBoundConstructor
	public ComposerTrigger(String spec, String php, String composerPhar, String composerJson) throws ANTLRException {
		super(spec);
		this.php = php;
		this.composerPhar = composerPhar;
		this.composerJson = composerJson;
	}

	@SuppressWarnings("unused")
	// called reflectively by XStream
	private ComposerTrigger() {
		this.php = "";
		this.composerPhar = "";
		this.composerJson = "";
	}

	@Override
	public void run() {
		Node node = super.job.getLastBuiltOn();

		if (node == null) {
			LOGGER.info("no previous build found so skip trigger");
			return;
		}
		if (StringUtils.isBlank(this.php)) {
			LOGGER.warning("php is not defined! skip trigger");
			return;
		}
		if (StringUtils.isBlank(this.composerPhar)) {
			LOGGER.warning("composer.phar is not defined! skip trigger");
			return;
		}

		StringBuilder workspace = new StringBuilder(super.job.getRootDir().getAbsolutePath());
		workspace = workspace.append(File.separator).append("workspace");
		if (StringUtils.isNotBlank(this.composerJson))
			workspace = workspace.append(File.separator).append(composerJson);

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			node.createLauncher(TaskListener.NULL).launch()
					.cmds(this.php, this.composerPhar, "update", "--dry-run", "-v", "--no-ansi")
					.pwd(workspace.toString()).stdout(baos).join();

			final String output = baos.toString();

			LOGGER.fine("output from composer.phar: " + output);

			if (StringUtils.indexOf(output, "Composer could not find a composer.json") >= 0) {
				LOGGER.warning("composer could not find a composer.json in directory: " + workspace.toString());
			} else if (StringUtils.indexOf(output, "Nothing to install") < 0) {
				job.scheduleBuild(0, new Cause() {
					@Override
					public String getShortDescription() {
						return "ComposerTrigger";
					}

					@Override
					public void print(TaskListener listener) {
						listener.getLogger().println();
						listener.getLogger().println("ComposerTrigger: one or more dependencies has an update...");
						listener.getLogger().println("Output from composer:");
						listener.getLogger().println("------------------------------");
						listener.getLogger().println(output.trim());
						listener.getLogger().println("------------------------------");
						listener.getLogger().println();
					}
				});
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getPhp() {
		return php;
	}

	public String getComposerJson() {
		return composerJson;
	}

	public String getComposerPhar() {
		return composerPhar;
	}

	/**
	 * {@link Converter} implementation for XStream. This converter uses the
	 * {@link PureJavaReflectionProvider}, which ensures that the default
	 * constructor is called.
	 */
	public static final class ConverterImpl extends RobustReflectionConverter {

		/**
		 * Class constructor.
		 * 
		 * @param mapper
		 *            the mapper
		 */
		public ConverterImpl(Mapper mapper) {
			super(mapper, new PureJavaReflectionProvider());
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
			return "check for dependency updates with composer";
		}
	}
}
