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

	private static Md5_gen gen = new Md5_gen();

	final private ArrayList<String> s_topics = new ArrayList<String>();

	private ArrayList<Commit> commits;
	private Repository local_repo;

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
	private void show_Commit(Commit commit) {
		System.out.println("\n--------------------------------------------------------------------------------");
		System.out.println("Message: " + commit.getMessage());
		System.out.println("--------------------------------------------------------------------------------");
		System.out.println("File modified: ");
		for (Item item : commit.getModified()) {
			System.out.println("\t* " + item.getName() + " - " + item.getChecksum() + " - " + item.getBytes().length + "bytes");
		}
		System.out.println("--------------------------------------------------------------------------------");
	}

	// Recupera la repository dalla DHT
	private FutureGet retrieve_Repository(String repo_name) {
		return dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();
	}

	// --------------------------------------------------------------
	// Implementazione metodi interfaccia
	// --------------------------------------------------------------
	public boolean createRepository(String repo_name, File directory) {
		try {
			// Recupero dalla DHT la repository
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			// Se ho successo ed è vuota
			if (futureGet.isSuccess() && futureGet.isEmpty()) {
				// Creo una repository
				dht.put(Number160.createHash(repo_name)).data(new Data(new Repository(repo_name, new HashSet<PeerAddress>(), directory))).start().awaitUninterruptibly();

				futureGet = this.retrieve_Repository(repo_name);

				// Recupero la repository dalla DHT
				this.local_repo = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Scarico i file dalla DHT
				for (Item file : this.local_repo.getItems()) {
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

	public boolean clone(String repo_name) {
		try {
			// Recupero dalla DHT la repository
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				// Recupero la repository dalla DHT
				this.local_repo = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Scarico i file dalla DHT
				for (Item file : this.local_repo.getItems()) {
					File dest = new File(repo_name + "/" + file.getName());
					FileUtils.writeByteArrayToFile(dest, file.getBytes());
				}

				// Aggiungo il nuovo iscritto
				this.local_repo.getUsers().add(dht.peer().peerAddress());

				// Ripubblico la lista sul topic
				dht.put(Number160.createHash(repo_name)).data(new Data(this.local_repo)).start().awaitUninterruptibly();

				// Aggiungo alla lista locale dei miei topic
				s_topics.add(repo_name);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean addFilesToRepository(String repo_name, File[] files) {
		try {
			// Recupero dalla DHT la repository
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				// Recupero la repository dalla DHT
				this.local_repo = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Recupero la lista degli iscritti
				HashSet<PeerAddress> peers_on_topic = this.local_repo.getUsers();

				// Aggiungo i files alla repository
				this.local_repo.add_Files(files);

				// Ripubblico i file sul topic
				dht.put(Number160.createHash(repo_name)).data(new Data(this.local_repo)).start().awaitUninterruptibly();

				// Per ogni iscritto invio una notifica
				for (PeerAddress peer : peers_on_topic) {
					FutureDirect futureDirect = dht.peer().sendDirect(peer).object(this.local_repo).start();
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

	public boolean commit(String repo_name, String message) {
		try {
			// Recupero dalla DHT la repository
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				// Recupero la repository dalla DHT
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
				this.show_Commit(this.commits.get(this.commits.size() - 1));
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public String push(String repo_name) {
		// TODO Check su pull

		try {
			// Recupero dalla DHT la repository
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				// Recupero la repository dalla DHT
				Repository repository = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Aggiorna la repository con i commit
				for (Commit commit : this.commits) {
					System.out.println(commit.getMessage());
					repository.update_Files(commit);
				}

				// Reiserisce la repository
				dht.put(Number160.createHash(repo_name)).data(new Data(repository)).start().awaitUninterruptibly();
			}
			return "OK";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Errore";
	}

	public String pull(String repo_name) {
		// Parto dall'ultima aggiornata
		// Faccio le modifiche le comparo con l'ultima aggiornata per creare il commit
		// faccio il pull quindi comparo ultima aggiornata con quella in remote
		// Se nelle differenze c'è qualche modidica che è anche nel commit allora
		// conflitto
		// Risolvo conflitto e pusho tutto sovrascrivendo

		Repository remote_repo;

		try {
			// Recupero dalla DHT la repository
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				// Recupero la repository dalla DHT
				remote_repo = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Recupero i file attualmente presenti localmente
				File directory = new File(repo_name + "/");
				File[] local_files = directory.listFiles();

				// Cerco file modificati
				for (File file : local_files) {
					if (remote_repo.isModified(file)) {
						System.out.println("Conflitto trovato sul file: " + file.getName());
						// file.renameTo(new File(repo_name + "/local_" + file.getName()));
						// File dest = new File(repo_name + "/" + file.getName());
						// File di remoto
						// FileUtils.writeByteArrayToFile(dest, remnot());
						// modified.add(new Item(file.getName(), gen.md5_Of_File(file),
						// Files.readAllBytes(file.toPath())));

					}
				}

				// ? Mostra lo stato della repository
				this.show_Status(repo_name);
			}
			return "Tutt'appost";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Errore nel pull";

	}

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

	public boolean leaveNetwork() {
		for (String topic : new ArrayList<String>(s_topics))
			unsubscribeFromTopic(topic);
		dht.peer().announceShutdown().start().awaitUninterruptibly();
		return true;
	}
}