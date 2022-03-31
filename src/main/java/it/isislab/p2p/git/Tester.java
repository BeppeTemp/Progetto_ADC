package it.isislab.p2p.git;

import java.io.File;

import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
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

	private static void printMenu() {
		System.out.println("Menu: ");
		System.out.println("1 - Create Repository");
		System.out.println("2 - Clone");
		System.out.println("3 - Add file to a Repository");
		System.out.println("4 - Commit");
		System.out.println("5 - Push");
		System.out.println("6 - Pull");
		System.out.println("7 - UN SUBSCRIBE ON TOPIC");
		System.out.println("8 - Exit");
		System.out.println();
	}

	public static void main(String[] args) throws Exception {
		final CmdLineParser parser = new CmdLineParser(new Tester());

		try {
			parser.parseArgument(args);
			TextIO textIO = TextIoFactory.getTextIO();
			PublishSubscribeImpl peer = new PublishSubscribeImpl(id, master, new MessageListenerImpl(id));

			System.out.println("\nPeer: " + id + " on Master: " + master + " \n");

			boolean flag = true;
			String name;
			String path;
			File files;

			while (flag) {
				printMenu();
				int option = textIO.newIntInputReader().withMaxVal(6).withMinVal(1).read("Option");

				switch (option) {
				case 1:
					name = textIO.newStringInputReader().withDefaultValue("my_new_repository").read("Repository Name:");
					path = textIO.newStringInputReader().read("Directory Name:");
					files = new File(path);

					if (files.listFiles() != null)
						if (peer.createRepository(name, files))
							System.out.println("\nRepository \"" + name + "\" Successfully Created\n");
						else
							System.out.println("\nError in repository creation\n");
					else
						System.out.println("\nInvalid path\n");
					break;

				case 2:
					name = textIO.newStringInputReader().withDefaultValue("my_new_repository").read("Repository Name:");

					if (peer.clone(name))
						System.out.println("\nSuccessfully cloned \"" + name + "\"\n");
					else
						System.out.println("\nError in clone repository\n");
					break;

				case 3:
					name = textIO.newStringInputReader().withDefaultValue("my_new_repository").read("Repository Name:");
					path = textIO.newStringInputReader().read("Directory Name:");

					File[] directory_files = new File(path).listFiles();

					if (directory_files != null) {
						if (peer.addFilesToRepository(name, directory_files))
							System.out.println("\nSuccessfully added files on repository \"" + name + "\"\n");
						else
							System.out.println("\nError in files publish\n");
					} else {
						System.out.println("\nNessun file da aggiungere trovato nella directory\n");
					}

					break;

				case 4:
					name = textIO.newStringInputReader().withDefaultValue("my_new_repository").read("Repository Name:");
					String message = textIO.newStringInputReader().withDefaultValue("I changed something").read("Message:");

					if (peer.commit(name, message))
						System.out.println("\nCommit \"" + name + "\" Successfully Created\n");
					else
						System.out.println("\nNothing to commit\n");
					break;

				case 5:
					name = textIO.newStringInputReader().withDefaultValue("my_new_repository").read("Repository Name:");

					if (peer.push(name) != null)
						System.out.println("\nPush on repo \"" + name + "\" done\n");
					else
						System.out.println("\nError in push operation\n");
					break;

				case 6:
					name = textIO.newStringInputReader().withDefaultValue("my_new_repository").read("Repository Name:");

					if (peer.pull(name) != null)
						System.out.println("\nRepository \"" + name + "\" Successfully Created\n");
					else
						System.out.println("\nError in repository creation\n");
					break;

				case 7:
					name = textIO.newStringInputReader().withDefaultValue("my_new_repository").read("Repository Name:");

					if (peer.unsubscribeFromTopic(name))
						System.out.println("\nRepository \"" + name + "\" Successfully Created\n");
					else
						System.out.println("\nError in repository creation\n");
					break;

				case 8:
					if (peer.leaveNetwork()) {
						System.out.println("\nDisconnection completed\n");
						flag = false;
					} else
						System.out.println("\nError in repository creation\n");
					break;

				default:
					break;
				}
			}
		} catch (CmdLineException clEx) {
			System.err.println("ERROR: Unable to parse command-line options: " + clEx);
		}
	}
}