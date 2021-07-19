package ext.deere.mcad.mbdviewer;

import ext.deere.mcad.mbdviewer.view.swing.MBDViewerUI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import com.threerings.getdown.util.LaunchUtil;

import java.awt.*;
import java.io.File;
import java.util.Arrays;

@SpringBootApplication
public class MBDViewerApplication implements CommandLineRunner {
	@Autowired
	MBDViewerUI mbdViewerUI;
	
	public static void main(String[] args) {
		String[] newArgs = args;

		if (args.length > 0 && Boolean.getBoolean("com.threerings.getdown")) {
			final File appdir = new File(args[0]);		
			new Thread() {
				@Override public void run () {
					LaunchUtil.upgradeGetdown(new File(appdir, "MBDViewer-launcher-old.jar"),
							new File(appdir, "MBDViewer-launcher.jar"),
							new File(appdir, "MBDViewer-launcher-new.jar"));
				}
			}.start();
			newArgs = Arrays.copyOfRange(args,1, args.length);
		}
		
		
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(MBDViewerApplication.class)
				.web(WebApplicationType.NONE)
				.headless(false)
				.run(newArgs);
	}

	@Override
	public void run(String... args) throws Exception {
		EventQueue.invokeLater(() -> {
			mbdViewerUI.initializeUI();
		});
	}
}
