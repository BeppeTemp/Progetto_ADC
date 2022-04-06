package it.isislab.p2p.git.implementations;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;

import it.isislab.p2p.git.classes.Commit;
import it.isislab.p2p.git.classes.Generator;
import it.isislab.p2p.git.classes.Item;
import it.isislab.p2p.git.classes.Repository;
import it.isislab.p2p.git.interfaces.GitProtocol;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;

public class GitProtocolImpl implements GitProtocol {
	final private Peer peer;
	final private PeerDHT dht;
	final private int DEFAULT_MASTER_PORT = 4000;

	private ArrayList<Commit> commits;
	private HashMap<String, Item> added;

	private HashMap<String, Repository> local_repo;
	private HashMap<String, Path> my_repository;

	// Costruttore
	public GitProtocolImpl(int _id, String _master_peer) throws Exception {
		this.commits = new ArrayList<Commit>();
		this.added = new HashMap<String, Item>();
		this.my_repository = new HashMap<String, Path>();
		this.local_repo = new HashMap<String, Repository>();

		peer = new PeerBuilder(Number160.createHash(_id)).ports(DEFAULT_MASTER_PORT + _id).start();
		dht = new PeerBuilderDHT(peer).start();

		FutureBootstrap fb = peer.bootstrap().inetAddress(InetAddress.getByName(_master_peer)).ports(DEFAULT_MASTER_PORT).start();
		fb.awaitUninterruptibly();

		if (fb.isSuccess()) {
			peer.discover().peerAddress(fb.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
		} else {
			throw new Exception("Error in master peer bootstrap.");
		}
	}

	//TODO da mettere nell'interfaccia
	public void show_Local_Commits() {
		if (this.commits.size() != 0) {
			System.out.println(this.commits.size() + " commit, in coda:");
			for (Commit commit : commits) {
				System.out.println("\n--------------------------------------------------------------------------------");
				System.out.println("🔹 Messaggio: " + commit.getMessage());
				System.out.println("--------------------------------------------------------------------------------");
				System.out.println("File modificati: ");
				for (Item item : commit.getModified().values()) {
					System.out.println("\t🔸 " + item.getName() + " - " + item.getChecksum() + " - " + item.getBytes().length + " bytes");
				}
				System.out.println("--------------------------------------------------------------------------------");
				System.out.println("File aggiunti o sovrascritti: ");
				for (Item item : commit.getAdded().values()) {
					System.out.println("\t🔸 " + item.getName() + " - " + item.getChecksum() + " - " + item.getBytes().length + " bytes");
				}
				System.out.println("--------------------------------------------------------------------------------");
			}
		} else {
			System.out.println("\n⚠️ Nessun commit da visualizzare.\n");
		}
	}

	//TODO da mettere nell'interfaccia
	public void show_Remote_Repo(String repo_name) {
		try {
			FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				Repository remote_repo = (Repository) futureGet.dataMap().values().iterator().next().object();

				System.out.println("\n--------------------------------------------------------------------------------");
				System.out.println("🔹 Nome: " + remote_repo.getName());
				System.out.println("🔹 Versione: " + remote_repo.getVersion());
				System.out.println("🔹 Numero di commit: " + remote_repo.getCommits().size());
				System.out.println("--------------------------------------------------------------------------------");
				System.out.println("File contenuti: ");
				for (Item item : remote_repo.getItems().values()) {
					System.out.println("\t🔸 " + item.getName() + " - " + item.getChecksum() + " - " + item.getBytes().length + " bytes");
				}
				System.out.println("--------------------------------------------------------------------------------");
			} else {
				System.out.println("\nRepository non trovata ❌\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//TODO da mettere nell'interfaccia
	public void show_Local_Repo(String repo_name) {
		if (this.local_repo.containsKey(repo_name)) {
			Repository local_repo = this.local_repo.get(repo_name);

			System.out.println("\n--------------------------------------------------------------------------------");
			System.out.println("🔹 Nome: " + local_repo.getName());
			System.out.println("🔹 Versione: " + local_repo.getVersion());
			System.out.println("🔹 Numero di commit: " + local_repo.getCommits().size());
			System.out.println("--------------------------------------------------------------------------------");
			System.out.println("File contenuti: ");
			for (Item item : local_repo.getItems().values()) {
				System.out.println("\t🔸 " + item.getName() + " - " + item.getChecksum() + " - " + item.getBytes().length + " bytes");
			}
			System.out.println("--------------------------------------------------------------------------------");
		} else {
			System.out.println("\nRepository non trovata ❌\n");
		}
	}

	// Mostra i file aggiunti
	private void show_added() {
		System.out.println("\n--------------------------------------------------------------------------------");
		System.out.println("Sono stati aggiunti " + this.added.size() + " file: ");
		for (Item item : this.added.values()) {
			System.out.println("\t* " + item.getName() + " - " + item.getChecksum() + " - " + item.getBytes().length + " bytes");
		}
		System.out.println("--------------------------------------------------------------------------------");
	}

	// --------------------------------------------------------------
	// Implementazione metodi interfaccia
	// --------------------------------------------------------------
	@Override
	public boolean createRepository(String repo_name, Path start_dir) {
		try {
			FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();

			if (futureGet.isSuccess() && futureGet.isEmpty()) {
				Repository repository = new Repository(repo_name, new HashSet<PeerAddress>(), start_dir);

				// Iscrivo il peer alla repository
				repository.add_peer(dht.peer().peerAddress());

				// Creo una repository
				dht.put(Number160.createHash(repo_name)).data(new Data(repository)).start().awaitUninterruptibly();

				// Salvo la posizione della repository sul disco in locale
				this.my_repository.put(repo_name, start_dir);

				// Recupero la repository dalla DHT
				futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();
				Repository remote_repo = (Repository) futureGet.dataMap().values().iterator().next().object();
				this.local_repo.put(remote_repo.getName(), remote_repo);

				// ? Mostra lo stato della repository
				this.show_Remote_Repo(repo_name);

				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean clone(String repo_name, Path clone_dir) {
		try {
			FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				Repository remote_repo = (Repository) futureGet.dataMap().values().iterator().next().object();
				this.local_repo.put(remote_repo.getName(), remote_repo);

				// Scarico i file nella directory indicata
				for (Item file : this.local_repo.get(repo_name).getItems().values()) {
					File dest = new File(clone_dir.toString(), file.getName());
					FileUtils.writeByteArrayToFile(dest, file.getBytes());
				}

				// Aggiungo il nuovo iscritto
				this.local_repo.get(repo_name).add_peer(dht.peer().peerAddress());

				// Salvo la posizione della repository sul disco in locale
				this.my_repository.put(repo_name, clone_dir);

				dht.put(Number160.createHash(repo_name)).data(new Data(this.local_repo.get(repo_name))).start().awaitUninterruptibly();

				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean addFilesToRepository(String repo_name, Path add_dir) {
		this.added.clear();

		try {
			// Aggiungo i files alla repository
			for (File file : add_dir.toFile().listFiles()) {
				if (!this.local_repo.get(repo_name).contains(file)) {
					this.added.put(file.getName(), new Item(file.getName(), Generator.md5_Of_File(file), Files.readAllBytes(file.toPath())));
				}
			}

			// ? Mostra file aggiunti
			this.show_added();

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean commit(String repo_name, String msg) {
		try {
			// Recupero i file presenti nella repository locale
			File[] local_files = this.my_repository.get(repo_name).toFile().listFiles();

			// Cerco file modificati
			HashMap<String, Item> modified = new HashMap<String, Item>();
			for (File file : local_files) {
				if (this.local_repo.get(repo_name).isModified(file))
					modified.put(file.getName(), new Item(file.getName(), Generator.md5_Of_File(file), Files.readAllBytes(file.toPath())));
			}

			// Controllo se c'è stata almeno una modifica o un aggiunta
			if (modified.size() == 0 && this.added.size() == 0)
				return false;
			else
				// Aggiungo il commit alla coda
				this.commits.add(new Commit(msg, modified, this.added));

			// ? Mostra lo stato del commit
			this.show_Local_Commits();

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public String push(String repo_name) {
		try {
			FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				Repository remote_repo = (Repository) futureGet.dataMap().values().iterator().next().object();

				if (remote_repo.getVersion() == this.local_repo.get(repo_name).getVersion()) {

					// Aggiorna la repository con i commit
					for (Commit commit : this.commits) {
						System.out.println("Commit #" + this.local_repo.get(repo_name).getCommits().size() + " msg: " + commit.getMessage() + " elaborato");
						this.local_repo.get(repo_name).commit(commit);
					}
					this.commits.clear();
					this.added.clear();

					dht.put(Number160.createHash(repo_name)).data(new Data(this.local_repo.get(repo_name))).start().awaitUninterruptibly();

					return "\nPush sulla repository \"" + repo_name + "\" completato ✅\n";
				} else
					return "\n⚠️ Stato della repository cambiato, eseguire prima un Pull.";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "\nErrore nella fase di push ❌\n";
	}

	@Override
	public String pull(String repo_name) {
		try {
			FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				// Recupero la repository dalla DHT
				Repository remote_repo = (Repository) futureGet.dataMap().values().iterator().next().object();
				
				HashMap<String, Item> modified = new HashMap<String, Item>();

				// Se lo stato della repository è cambiata dal mio ultimo pull
				if (remote_repo.getVersion() > local_repo.get(repo_name).getVersion()) {
					File[] local_files = this.my_repository.get(repo_name).toFile().listFiles();

					// Identifico tutti i file da me modificati fino a questo momento
					for (File file : local_files) {
						if (this.local_repo.get(repo_name).isModified(file))
							modified.put(file.getName(), new Item(file.getName(), Generator.md5_Of_File(file), Files.readAllBytes(file.toPath())));
					}

					// Se i file da me modificati sono diversi da quelli contenuti in remoto
					for (Item item : modified.values()) {
						if (remote_repo.isModified(item)) {
							// Ne genero due copie
							File remote_dest = new File(this.my_repository.get(repo_name).toString(), "/REMOTE-" + item.getName());
							FileUtils.writeByteArrayToFile(remote_dest, remote_repo.getItems().get(item.getName()).getBytes());

							File local_dest = new File(this.my_repository.get(repo_name).toString(), "/LOCAL-" + item.getName());
							File local_modified = new File(this.my_repository.get(repo_name).toString(), item.getName());
							local_modified.renameTo(local_dest);

							System.out.println("\n⚠️ Identificato conflitto sul file: " + item.getName());

							// Attendo che l'utente risolve il conflitto
							TextIO textIO = TextIoFactory.getTextIO();
							while (local_files.length != this.my_repository.get(repo_name).toFile().listFiles().length)
								textIO.newCharInputReader().read("Eliminare uno dei due file per risolvere il conflitto, dopodichè dare invio.");

							// Non faccio nient'altro in quanto sarà il commit a identificarli nuovamente
							// come modificati
						}
					}
				}

				this.local_repo.get(repo_name).setVersion(remote_repo.getVersion());

				// TODO possibile refactoring di questa parte (Se funge)
				// Per ogni fine della repositore remota, non modificato da me, ne aggiorno lo
				// stato in quella locale
				for (Item item : remote_repo.getItems().values()) {
					// Se l'item è contenuto localmente
					if (this.local_repo.get(repo_name).getItems().containsKey(item.getName())) {
						// e non è uno dei modificati
						if (!modified.containsKey(item.getName())) {
							// ne aggiorno il contenuto sia nella repository locale
							this.local_repo.get(repo_name).getItems().get(item.getName()).setBytes(item.getBytes());

							// che come file
							File need_update = new File(this.my_repository.get(repo_name).toString(), item.getName());
							FileUtils.writeByteArrayToFile(need_update, item.getBytes());
						}
					} else {
						// Se non è contenuto localemente lo aggiungo alla repository locale
						this.local_repo.get(repo_name).getItems().put(item.getName(), item);

						// e come file
						File need_add = new File(this.my_repository.get(repo_name).toString(), item.getName());
						FileUtils.writeByteArrayToFile(need_add, item.getBytes());
					}
				}

				// Se non ho modificato nessun file tutti verranno aggiornati o aggiunti e le
				// repository locale e remota saranno sincronizzate
			}
			return "\nPull della repository \"" + repo_name + "\" completato ✅\n";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "\nErrore nella fase di pull ❌\n";
	}

	@Override
	public boolean removeRepo(String repo_name) {
		try {
			FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();
			if (futureGet.isSuccess()) {
				if (futureGet.isEmpty())
					return false;

				Repository remote_repo = (Repository) futureGet.dataMap().values().iterator().next().object();
				remote_repo.remove_peer(dht.peer().peerAddress());

				dht.put(Number160.createHash(repo_name)).data(new Data(remote_repo)).start().awaitUninterruptibly();

				TextIO textIO = TextIoFactory.getTextIO();
				String value = textIO.newStringInputReader().read("Y, per elimanare i file, N per mantenerli: ");

				if (value.toUpperCase().compareTo("Y") == 0) {
					File repo_dir = this.my_repository.get(repo_name).toFile();
					repo_dir.delete();
				}

				my_repository.remove(repo_name);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean leaveNetwork() {
		for (String repo_name : this.my_repository.keySet()) {
			this.removeRepo(repo_name);
		}
		dht.peer().announceShutdown().start().awaitUninterruptibly();
		return true;
	}
}