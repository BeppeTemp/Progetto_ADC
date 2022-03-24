package it.isislab.p2p.git;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import it.isislab.p2p.git.implementations.MessageListenerImpl;
import it.isislab.p2p.git.implementations.PublishSubscribeImpl;

/**
 * docker build --no-cache -t test . docker run -i -e MASTERIP="127.0.0.1" -e
 * ID=0 test use -i for interactive mode use -e to set the environment variables
 * 
 * @author carminespagnuolo
 *
 */
public class Tester {

	@Option(name = "-m", aliases = "--masterip", usage = "the master peer ip address", required = true)
	private static String master;

	@Option(name = "-id", aliases = "--identifierpeer", usage = "the unique identifier for this peer", required = true)
	private static int id;

	public static void main(String[] args) throws Exception {
		final CmdLineParser parser = new CmdLineParser(new Tester());

		try {
			parser.parseArgument(args);

			TextIO textIO = TextIoFactory.getTextIO();

			TextTerminal terminal = textIO.getTextTerminal();
			PublishSubscribeImpl peer = new PublishSubscribeImpl(id, master, new MessageListenerImpl(id));

			terminal.printf("\nPeer: %d on Master: %s \n", id, master);

			while (true) {
				printMenu(terminal);

				int option = textIO.newIntInputReader().withMaxVal(5).withMinVal(1).read("Option");
				switch (option) {
				case 1:
					//TODO Debug da rimuovere
					File file = new File("repository");

					ArrayList<File> files = new ArrayList<File>();
					Collections.addAll(files, file.listFiles());

					System.out.print(files.size());
					System.out.print(files.get(0).getName());
					System.out.print(files.get(1).getName());

					String name = textIO.newStringInputReader().withDefaultValue("default-topic").read("Repository Name:");
					if (peer.createRepository(name, file))
						terminal.printf("\nRepository %s Successfully Created \n", name);
					else
						terminal.printf("\nError in repository creation \n");
					break;
				case 2:
					terminal.printf("\nENTER TOPIC NAME\n");
					String sname = textIO.newStringInputReader().withDefaultValue("default-topic").read("Name:");
					if (peer.subscribetoTopic(sname))
						terminal.printf("\n SUCCESSFULLY SUBSCRIBED TO %s\n", sname);
					else
						terminal.printf("\nERROR IN TOPIC SUBSCRIPTION\n");
					break;
				case 4:
					terminal.printf("\nENTER TOPIC NAME\n");
					String tname = textIO.newStringInputReader().withDefaultValue("default-topic").read(" Name:");
					terminal.printf("\nENTER MESSAGE\n");
					String message = textIO.newStringInputReader().withDefaultValue("default-message").read(" Message:");
					if (peer.publishToTopic(tname, message))
						terminal.printf("\n SUCCESSFULLY PUBLISH MESSAGE ON TOPIC %s\n", tname);
					else
						terminal.printf("\nERROR IN TOPIC PUBLISH\n");

					break;
				case 3:
					terminal.printf("\nENTER TOPIC NAME\n");
					String uname = textIO.newStringInputReader().withDefaultValue("default-topic").read("Name:");
					if (peer.unsubscribeFromTopic(uname))
						terminal.printf("\n SUCCESSFULLY UNSUBSCRIBED TO %s\n", uname);
					else
						terminal.printf("\nERROR IN TOPIC UN SUBSCRIPTION\n");
					break;
				case 5:
					terminal.printf("\nARE YOU SURE TO LEAVE THE NETWORK?\n");
					boolean exit = textIO.newBooleanInputReader().withDefaultValue(false).read("exit?");
					if (exit) {
						peer.leaveNetwork();
						System.exit(0);
					}
					break;

				default:
					break;
				}
			}

		} catch (CmdLineException clEx) {
			System.err.println("ERROR: Unable to parse command-line options: " + clEx);
		}

	}

	public static void printMenu(TextTerminal terminal) {
		terminal.printf("\nMenu: \n");
		terminal.printf("1 - Create Repository\n");
		terminal.printf("2 - SUBSCRIBE TOPIC\n");
		terminal.printf("3 - UN SUBSCRIBE ON TOPIC\n");
		terminal.printf("4 - PUBLISH ON TOPIC\n");
		terminal.printf("5 - Exit\n");

	}

}
