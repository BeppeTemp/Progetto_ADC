package it.isislab.p2p.git.implementations;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;

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

	final private ArrayList<String> s_topics = new ArrayList<String>();

	// Costruttore
	public PublishSubscribeImpl(int _id, String _master_peer, final MessageListener _listener) throws Exception {
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

		ArrayList<File> files = ((Repository) futureGet.dataMap().values().iterator().next().object()).getFiles();

		System.out.println("\n----------------------------------");
		System.out.println("Files in the repository: ");
		for (File file : files) {
			System.out.println("\t* " + file.getName());
		}
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
				dht.put(Number160.createHash(repo_name)).data(new Data(new Repository(repo_name, directory, new HashSet<PeerAddress>()))).start().awaitUninterruptibly();

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
				Repository repository = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Scarico i file dalla DHT
				for (File file : repository.getFiles()) {
					File dest = new File(repo_name + "/" + file.getName());
					FileUtils.copyFile(file, dest);
				}

				// Aggiungo il nuovo iscritto
				repository.getUsers().add(dht.peer().peerAddress());

				// Ripubblico la lista sul topic
				dht.put(Number160.createHash(repo_name)).data(new Data(repository)).start().awaitUninterruptibly();

				// Aggiungo alla lista locale dei miei topic
				s_topics.add(repo_name);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean addFilesToRepository(String repo_name, ArrayList<File> files) {
		try {
			// Recupero dalla DHT la repository
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				// Recupero la repository dalla DHT
				Repository repository = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Recupero la lista degli iscritti
				HashSet<PeerAddress> peers_on_topic = repository.getUsers();

				// Aggiungo i files alla repository
				repository.add_Files(files);

				// Ripubblico i file sul topic
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
	public boolean commit(String repo_name, String _message) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String push(String repo_name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String pull(String repo_name) {
		try {
			// Recupero dalla DHT la repository
			FutureGet futureGet = this.retrieve_Repository(repo_name);

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				// Recupero la repository dalla DHT
				Repository repository = (Repository) futureGet.dataMap().values().iterator().next().object();

				// Scarico i file dalla DHT
				for (File file : repository.getFiles()) {
					File dest = new File(repo_name + "/" + file.getName());
					FileUtils.copyFile(file, dest);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "Operazione completata con successo";
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