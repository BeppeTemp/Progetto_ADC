package it.isislab.p2p.git.implementations;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;

import it.isislab.p2p.git.beans.Commit;
import it.isislab.p2p.git.beans.Item;
import it.isislab.p2p.git.beans.Md5_gen;
import it.isislab.p2p.git.beans.Repository;
import it.isislab.p2p.git.interfaces.MessageListener;
import it.isislab.p2p.git.interfaces.PublishSubscribe;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

public class PublishSubscribeImpl implements PublishSubscribe {
	final private Peer peer;
	final private PeerDHT dht;
	final private int DEFAULT_MASTER_PORT = 4000;

	private ArrayList<Commit> commits;
	private static Md5_gen gen = new Md5_gen();

	final private ArrayList<String> s_topics = new ArrayList<String>();

	// Costruttore
	public PublishSubscribeImpl(int _id, String _master_peer, final MessageListener _listener) throws Exception {
		this.commits = new ArrayList<Commit>();

		peer = new PeerBuilder(Number160.createHash(_id)).ports(DEFAULT_MASTER_PORT + _id).start();
		dht = new PeerBuilderDHT(peer).start();

		FutureBootstrap fb = peer.bootstrap().inetAddress(InetAddress.getByName(_master_peer)).ports(DEFAULT_MASTER_PORT).start();
		fb.awaitUninterruptibly();

		if (fb.isSuccess()) {
			peer.discover().peerAddress(fb.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
		} else {
			throw new Exception("Error in master peer bootstrap.");
		}

		peer.objectDataReply(new ObjectDataReply() {
			public Object reply(PeerAddress sender, Object request) throws Exception {
				return _listener.parseMessage(request);
			}
		});
	}

	// Mostra lo stato corrente della repository
	private void show_Status(String repo_name) throws ClassNotFoundException, IOException {
		FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start();
		futureGet.awaitUninterruptibly();

		ArrayList<Item> items = ((Repository) futureGet.dataMap().values().iterator().next().object()).getItems();

		System.out.println("\n--------------------------------------------------------------------------------");
		System.out.println("Name: " + repo_name);
		System.out.println("--------------------------------------------------------------------------------");
		System.out.println("File stored: ");
		for (Item item : items) {
			System.out.println("\t* " + item.getName() + " - " + item.getChecksum() + " - " + item.getBytes().length + "bytes");
		}
		System.out.println("--------------------------------------------------------------------------------");
	}

	// Mostra lo stato del commit
	private void show_Commit() {
		System.out.println(this.commits.size() + " commit, in coda:");
		for (Commit commit : commits) {
			System.out.println("\n--------------------------------------------------------------------------------");
			System.out.println("Message: " + commit.getMessage());
			System.out.println("--------------------------------------------------------------------------------");
			System.out.println("File modified: ");
			for (Item item : commit.getModified()) {
				System.out.println("\t* " + item.getName() + " - " + item.getChecksum() + " - " + item.getBytes().length + "bytes");
			}
			System.out.println("--------------------------------------------------------------------------------");
		}
	}

	// Recupera la repository dalla DHT
	private FutureGet retrieve_Repository(String repo_name) {
		return dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();
	}

	// --------------------------------------------------------------
	// Implementazione metodi interfaccia
	// --------------------------------------------------------------
	@Override
	public boolean createRepository(String repo_name, File directory) {
		try {
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			if (futureGet.isSuccess() && futureGet.isEmpty()) {
				// Creo una repository
				dht.put(Number160.createHash(repo_name)).data(new Data(new Repository(repo_name, new HashSet<PeerAddress>(), directory))).start().awaitUninterruptibly();

				futureGet = this.retrieve_Repository(repo_name);

				// Recupero la repository dalla DHT
				Repository repository = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Scarico i file dalla DHT
				for (Item file : repository.getItems()) {
					File dest = new File(repo_name + "/" + file.getName());
					FileUtils.writeByteArrayToFile(dest, file.getBytes());
				}

				// ? Mostra lo stato della repository
				this.show_Status(repo_name);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean clone(String repo_name) {
		try {
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				Repository repository = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Scarico i file dalla DHT
				for (Item file : repository.getItems()) {
					File dest = new File(repo_name + "/" + file.getName());
					FileUtils.writeByteArrayToFile(dest, file.getBytes());
				}

				// Aggiungo il nuovo iscritto
				repository.getUsers().add(dht.peer().peerAddress());

				dht.put(Number160.createHash(repo_name)).data(new Data(repository)).start().awaitUninterruptibly();
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean addFilesToRepository(String repo_name, File[] files) {
		try {
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				Repository repository = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Recupero la lista degli iscritti
				HashSet<PeerAddress> peers_on_topic = repository.getUsers();

				// Aggiungo i files alla repository
				for (File file : files) {
					if (repository.contains(file) == -1) {
						System.out.println("Aggiunto: " + file.getName());
						repository.add_Item(new Item(file.getName(), gen.md5_Of_File(file), Files.readAllBytes(file.toPath())));
					}
				}

				dht.put(Number160.createHash(repo_name)).data(new Data(repository)).start().awaitUninterruptibly();

				// Per ogni iscritto invio una notifica
				for (PeerAddress peer : peers_on_topic) {
					FutureDirect futureDirect = dht.peer().sendDirect(peer).object(repository).start();
					futureDirect.awaitUninterruptibly();
				}

				// ? Mostra lo stato della repository
				this.show_Status(repo_name);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean commit(String repo_name, String message) {
		try {
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				Repository repository = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Recupero i file attualmente presenti localmente
				File directory = new File(repo_name + "/");
				File[] local_files = directory.listFiles();

				// Cerco file modificati
				ArrayList<Item> modified = new ArrayList<Item>();
				for (File file : local_files) {
					if (repository.isModified(file))
						modified.add(new Item(file.getName(), gen.md5_Of_File(file), Files.readAllBytes(file.toPath())));
				}

				if (modified.size() == 0)
					return false;
				else
					this.commits.add(new Commit(message, modified));

				// ? Mostra lo stato del commit
				this.show_Commit();
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public String push(String repo_name) {
		// TODO Check su pull

		try {
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				Repository repository = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Aggiorna la repository con i commit
				for (Commit commit : this.commits) {
					System.out.println("Commit #" + repository.getCommits().size() + " msg: " + commit.getMessage() + " elaborato");
					repository.commit(commit);
					//TODO svuotare la lista
				}

				dht.put(Number160.createHash(repo_name)).data(new Data(repository)).start().awaitUninterruptibly();
				return "\nPush sulla repository \"" + repo_name + "\" completato ✅\n";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "\nErrore nella fase di push ❌\n";
	}

	@Override
	public String pull(String repo_name) {
		// Parto dall'ultima aggiornata
		// Faccio le modifiche le comparo con l'ultima aggiornata per creare il commit
		// faccio il pull quindi comparo ultima aggiornata con quella in remote
		// Se nelle differenze c'è qualche modidica che è anche nel commit allora
		// conflitto
		// Risolvo conflitto e pusho tutto sovrascrivendo

		try {
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				// Recupero la repository dalla DHT
				Repository repository = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Scarico i file dalla DHT
				for (Item file : repository.getItems()) {
					File dest = new File(repo_name + "/" + file.getName());
					FileUtils.writeByteArrayToFile(dest, file.getBytes());
					// TODO implementare i conflitti
				}

				// TODO Mostrare i cambiamenti
			}
			return "\nPull della repository \"" + repo_name + "\" completato ✅\n";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "\nErrore nella fase di pull ❌\n";
	}

	@Override
	public boolean unsubscribeFromTopic(String _topic_name) {
		try {
			FutureGet futureGet = dht.get(Number160.createHash(_topic_name)).start();
			futureGet.awaitUninterruptibly();
			if (futureGet.isSuccess()) {
				if (futureGet.isEmpty())
					return false;
				HashSet<PeerAddress> peers_on_topic;
				peers_on_topic = (HashSet<PeerAddress>) futureGet.dataMap().values().iterator().next().object();
				peers_on_topic.remove(dht.peer().peerAddress());
				dht.put(Number160.createHash(_topic_name)).data(new Data(peers_on_topic)).start().awaitUninterruptibly();
				s_topics.remove(_topic_name);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean leaveNetwork() {
		for (String topic : new ArrayList<String>(s_topics))
			unsubscribeFromTopic(topic);
		dht.peer().announceShutdown().start().awaitUninterruptibly();
		return true;
	}
}