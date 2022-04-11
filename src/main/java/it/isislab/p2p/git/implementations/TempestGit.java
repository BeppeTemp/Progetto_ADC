package it.isislab.p2p.git.implementations;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import org.apache.commons.io.FileUtils;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;

import it.isislab.p2p.git.entity.Commit;
import it.isislab.p2p.git.entity.Generator;
import it.isislab.p2p.git.entity.Item;
import it.isislab.p2p.git.entity.Repository;
import it.isislab.p2p.git.exceptions.NothingToPushException;
import it.isislab.p2p.git.exceptions.RepoStateChangedException;
import it.isislab.p2p.git.exceptions.RepositoryAlreadyExistException;
import it.isislab.p2p.git.exceptions.RepositoryNotExist;
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

public class TempestGit implements GitProtocol {
	final private Peer peer;
	final private PeerDHT dht;
	final private int DEFAULT_MASTER_PORT = 4000;

	private HashMap<String, Repository> local_repos;
	private HashMap<String, Item> local_added;
	private HashMap<String, ArrayList<Commit>> local_commits;
	private HashMap<String, Path> my_repos;

	public TempestGit(int _id, String _master_peer) throws Exception {
		this.local_repos = new HashMap<String, Repository>();
		this.local_added = new HashMap<String, Item>();
		this.local_commits = new HashMap<String, ArrayList<Commit>>();
		this.my_repos = new HashMap<String, Path>();

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

	@Override
	public boolean createRepository(String repo_name, Path start_dir, Path repo_dir) throws Exception {

		FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();

		if (futureGet.isSuccess()) {
			if (!futureGet.isEmpty()) {
				throw new RepositoryAlreadyExistException();
			}

			Repository repository = new Repository(repo_name, peer.p2pId(), new HashSet<PeerAddress>(), start_dir);
			repository.add_peer(dht.peer().peerAddress());

			dht.put(Number160.createHash(repo_name)).data(new Data(repository)).start().awaitUninterruptibly();
			this.clone(repo_name, repo_dir);

			return true;
		}

		return false;
	}

	@Override
	public boolean clone(String repo_name, Path clone_dir) {
		try {
			FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();

			if (futureGet.isSuccess() && !futureGet.isEmpty()) {
				Repository remote_repo = (Repository) futureGet.dataMap().values().iterator().next().object();
				this.local_repos.put(remote_repo.getName(), remote_repo);

				for (Item file : this.local_repos.get(repo_name).getItems().values()) {
					File dest = new File(clone_dir.toString(), file.getName());
					FileUtils.writeByteArrayToFile(dest, file.getBytes());
				}

				this.local_repos.get(repo_name).add_peer(dht.peer().peerAddress());
				this.local_commits.put(repo_name, new ArrayList<Commit>());
				this.my_repos.put(repo_name, clone_dir);

				dht.put(Number160.createHash(repo_name)).data(new Data(this.local_repos.get(repo_name))).start().awaitUninterruptibly();

				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Collection<Item> addFilesToRepository(String repo_name, Path add_dir) {
		this.local_added.clear();

		try {
			for (File file : add_dir.toFile().listFiles()) {
				if (!this.local_repos.get(repo_name).contains(file)) {
					this.local_added.put(file.getName(), new Item(file.getName(), Generator.md5_Of_File(file), Files.readAllBytes(file.toPath())));
				}
			}

			return this.local_added.values();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean commit(String repo_name, String msg) {
		try {
			File[] local_files = this.my_repos.get(repo_name).toFile().listFiles();

			HashMap<String, Item> modified = new HashMap<String, Item>();
			for (File file : local_files) {
				if (this.local_repos.get(repo_name).isModified(file))
					modified.put(file.getName(), new Item(file.getName(), Generator.md5_Of_File(file), Files.readAllBytes(file.toPath())));
			}

			if (modified.size() == 0 && this.local_added.size() == 0)
				return false;
			else
				this.local_commits.get(repo_name).add(new Commit(msg, modified, this.local_added));

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Boolean push(String repo_name) throws RepoStateChangedException, NothingToPushException, RepositoryNotExist, ClassNotFoundException, IOException {

		FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();

		if (futureGet.isSuccess() && !futureGet.isEmpty()) {
			if (this.local_commits.get(repo_name).size() != 0) {
				Repository remote_repo = (Repository) futureGet.dataMap().values().iterator().next().object();

				if (remote_repo.getVersion() == this.local_repos.get(repo_name).getVersion()) {

					for (Commit commit : this.local_commits.get(repo_name)) {
						this.local_repos.get(repo_name).commit(commit);
					}
					this.local_commits.get(repo_name).clear();
					this.local_added.clear();

					dht.put(Number160.createHash(repo_name)).data(new Data(this.local_repos.get(repo_name))).start().awaitUninterruptibly();

					return true;
				} else
				throw new RepoStateChangedException();
			} else {
				throw new NothingToPushException();
			}
		} else {
			throw new RepositoryNotExist();
		}
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
				if (remote_repo.getVersion() > local_repos.get(repo_name).getVersion()) {
					File[] local_files = this.my_repos.get(repo_name).toFile().listFiles();

					// Identifico tutti i file da me modificati fino a questo momento
					for (File file : local_files) {
						if (this.local_repos.get(repo_name).isModified(file))
							modified.put(file.getName(), new Item(file.getName(), Generator.md5_Of_File(file), Files.readAllBytes(file.toPath())));
					}

					// Se i file da me modificati sono diversi da quelli contenuti in remoto
					for (Item item : modified.values()) {
						if (remote_repo.isModified(item)) {
							// Ne genero due copie
							File remote_dest = new File(this.my_repos.get(repo_name).toString(), "/REMOTE-" + item.getName());
							FileUtils.writeByteArrayToFile(remote_dest, remote_repo.getItems().get(item.getName()).getBytes());

							File local_dest = new File(this.my_repos.get(repo_name).toString(), "/LOCAL-" + item.getName());
							File local_modified = new File(this.my_repos.get(repo_name).toString(), item.getName());
							local_modified.renameTo(local_dest);

							System.out.println("\n⚠️ Identificato conflitto sul file: " + item.getName() + "\n");

							// Attendo che l'utente risolve il conflitto
							TextIO textIO = TextIoFactory.getTextIO();
							while (local_files.length != this.my_repos.get(repo_name).toFile().listFiles().length)
								textIO.newCharInputReader().read("Eliminare uno dei due file per risolvere il conflitto, dopodichè dare invio.");

							String message = textIO.newStringInputReader().withDefaultValue("Risolto conflitto sul file: " + item.getName()).read("Messaggio del nuovo commit:");
							this.commit(repo_name, message);

							// Non faccio nient'altro in quanto sarà il commit a identificarli nuovamente
							// come modificati
						}
					}
				}

				this.local_repos.get(repo_name).setVersion(remote_repo.getVersion());

				// TODO possibile refactoring di questa parte (Se funge)
				// Per ogni fine della repositore remota, non modificato da me, ne aggiorno lo
				// stato in quella locale
				for (Item item : remote_repo.getItems().values()) {
					// Se l'item è contenuto localmente
					if (this.local_repos.get(repo_name).getItems().containsKey(item.getName())) {
						// e non è uno dei modificati
						if (!modified.containsKey(item.getName())) {
							// ne aggiorno il contenuto sia nella repository locale
							this.local_repos.get(repo_name).getItems().get(item.getName()).setBytes(item.getBytes());

							// che come file
							File need_update = new File(this.my_repos.get(repo_name).toString(), item.getName());
							FileUtils.writeByteArrayToFile(need_update, item.getBytes());
						}
					} else {
						// Se non è contenuto localemente lo aggiungo alla repository locale
						this.local_repos.get(repo_name).getItems().put(item.getName(), item);

						// e come file
						File need_add = new File(this.my_repos.get(repo_name).toString(), item.getName());
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

				if (remote_repo.getOwner() == peer.p2pId()) {
					dht.remove(Number160.createHash(repo_name)).start().awaitUninterruptibly();
				} else {
					remote_repo.remove_peer(dht.peer().peerAddress());
					dht.put(Number160.createHash(repo_name)).data(new Data(remote_repo)).start().awaitUninterruptibly();
				}

				// ! rimuove solo al possessore
				File repo_dir = this.my_repos.get(repo_name).toFile();
				for (File file : repo_dir.listFiles()) {
					file.delete();
				}
				repo_dir.delete();

				this.local_repos.remove(repo_name);
				this.local_commits.remove(repo_name);
				this.my_repos.remove(repo_name);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean leaveNetwork() {
		for (String repo_name : this.my_repos.keySet()) {
			this.removeRepo(repo_name);
		}
		dht.peer().announceShutdown().start().awaitUninterruptibly();
		return true;
	}

	@Override
	public ArrayList<Commit> get_local_commits(String repo_name) {
		if (this.local_commits.get(repo_name) != null)
			if (this.local_commits.get(repo_name).size() != 0)
				return this.local_commits.get(repo_name);
		return null;
	}

	@Override
	public Repository get_local_repo(String repo_name) {
		return this.local_repos.get(repo_name);
	}

	@Override
	public Repository get_remote_repo(String repo_name) throws Exception {
		FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();

		if (futureGet.isSuccess() && !futureGet.isEmpty())
			return (Repository) futureGet.dataMap().values().iterator().next().object();
		return null;
	}
}