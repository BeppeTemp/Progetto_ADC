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

import org.apache.commons.io.FileUtils;

import it.isislab.p2p.git.entity.Commit;
import it.isislab.p2p.git.entity.Generator;
import it.isislab.p2p.git.entity.Item;
import it.isislab.p2p.git.entity.Repository;
import it.isislab.p2p.git.exceptions.GeneratedConflitException;
import it.isislab.p2p.git.exceptions.NothingToPushException;
import it.isislab.p2p.git.exceptions.RepoStateChangedException;
import it.isislab.p2p.git.exceptions.RepositoryAlreadyExistException;
import it.isislab.p2p.git.exceptions.RepositoryNotExistException;
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

	@Override
	public ArrayList<Commit> get_local_commits(String repo_name) {
		if (this.local_commits.get(repo_name) != null)
			if (this.local_commits.get(repo_name).size() != 0)
				return this.local_commits.get(repo_name);
		return null;
	}

	@Override
	public boolean createRepository(String repo_name, Path start_dir, Path repo_dir) throws RepositoryAlreadyExistException {

		FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();

		try {
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
		} catch (IOException e) {
			e.printStackTrace();
		} catch (RepositoryNotExistException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean clone(String repo_name, Path clone_dir) throws RepositoryNotExistException {

		FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();

		if (futureGet.isSuccess())
			if (!futureGet.isEmpty()) {
				try {
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
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				throw new RepositoryNotExistException();
			}

		return false;
	}

	@Override
	public Collection<Item> addFilesToRepository(String repo_name, Path add_dir) {
		this.local_added.clear();

		try {
			File files[] = add_dir.toFile().listFiles();
			if (files != null) {
				for (File file : files) {
					if (!this.local_repos.get(repo_name).contains(file)) {
						this.local_added.put(file.getName(), new Item(file.getName(), Generator.md5_Of_File(file), Files.readAllBytes(file.toPath())));
					}
				}

				return this.local_added.values();
			} else
				return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Commit commit(String repo_name, String msg) {
		try {
			File[] local_files = this.my_repos.get(repo_name).toFile().listFiles();

			HashMap<String, Item> modified = new HashMap<String, Item>();
			for (File file : local_files) {
				if (this.local_repos.get(repo_name).isModified(file))
					modified.put(file.getName(), new Item(file.getName(), Generator.md5_Of_File(file), Files.readAllBytes(file.toPath())));
			}

			if (modified.size() == 0 && this.local_added.size() == 0)
				return null;
			else
				this.local_commits.get(repo_name).add(new Commit(msg, modified, this.local_added));

			return this.local_commits.get(repo_name).get(this.local_commits.get(repo_name).size() - 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Boolean push(String repo_name) throws RepoStateChangedException, NothingToPushException, RepositoryNotExistException {

		FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();

		if (futureGet.isSuccess())
			if (!futureGet.isEmpty()) {
				if (this.local_commits.get(repo_name).size() != 0) {
					Repository remote_repo = null;

					try {
						remote_repo = (Repository) futureGet.dataMap().values().iterator().next().object();
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}

					if (remote_repo.getVersion() == this.local_repos.get(repo_name).getVersion()) {

						for (Commit commit : this.local_commits.get(repo_name)) {
							this.local_repos.get(repo_name).commit(commit);
						}
						this.local_commits.get(repo_name).clear();
						this.local_added.clear();

						try {
							dht.put(Number160.createHash(repo_name)).data(new Data(this.local_repos.get(repo_name))).start().awaitUninterruptibly();
						} catch (IOException e) {
							e.printStackTrace();
						}

						return true;
					} else
						throw new RepoStateChangedException();

				} else {
					throw new NothingToPushException();
				}
			} else {
				throw new RepositoryNotExistException();
			}
		return false;
	}

	@Override
	public Boolean pull(String repo_name) throws RepositoryNotExistException, GeneratedConflitException {
		FutureGet futureGet = dht.get(Number160.createHash(repo_name)).start().awaitUninterruptibly();

		if (futureGet.isSuccess())
			if (!futureGet.isEmpty()) {
				Repository remote_repo = null;

				try {
					remote_repo = (Repository) futureGet.dataMap().values().iterator().next().object();
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}

				HashMap<String, Item> modified = new HashMap<String, Item>();

				if (remote_repo.getVersion() > local_repos.get(repo_name).getVersion()) {
					File[] local_files = this.my_repos.get(repo_name).toFile().listFiles();

					for (File file : local_files) {
						if (this.local_repos.get(repo_name).isModified(file))
							try {
								modified.put(file.getName(), new Item(file.getName(), Generator.md5_Of_File(file), Files.readAllBytes(file.toPath())));
							} catch (IOException e) {
								e.printStackTrace();
							}
					}

					check_Conflit(repo_name, remote_repo, modified, local_files);
				}
				this.local_repos.get(repo_name).setVersion(remote_repo.getVersion());
				
				update_repo(repo_name, remote_repo, modified);
				return true;
			} else {
				throw new RepositoryNotExistException();
			}
		return false;
	}

	private void check_Conflit(String repo_name, Repository remote_repo, HashMap<String, Item> modified, File[] local_files) throws GeneratedConflitException {
		for (Item item : modified.values()) {
			if (remote_repo.isModified(item)) {

				File remote_dest = new File(this.my_repos.get(repo_name).toString(), "/REMOTE-" + item.getName());

				try {
					FileUtils.writeByteArrayToFile(remote_dest, remote_repo.getItems().get(item.getName()).getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}

				File local_dest = new File(this.my_repos.get(repo_name).toString(), "/LOCAL-" + item.getName());
				File local_modified = new File(this.my_repos.get(repo_name).toString(), item.getName());
				local_modified.renameTo(local_dest);

				throw new GeneratedConflitException();
			}
		}
	}

	private void update_repo(String repo_name, Repository remote_repo, HashMap<String, Item> modified) {
		for (Item item : remote_repo.getItems().values()) {
			if (this.local_repos.get(repo_name).getItems().containsKey(item.getName())) {
				if (!modified.containsKey(item.getName())) {
					this.local_repos.get(repo_name).getItems().get(item.getName()).setBytes(item.getBytes());
				}
			} else {
				this.local_repos.get(repo_name).getItems().put(item.getName(), item);
			}

			File need_add = new File(this.my_repos.get(repo_name).toString(), item.getName());

			try {
				FileUtils.writeByteArrayToFile(need_add, item.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean leaveNetwork() {
		for (String repo_name : this.my_repos.keySet()) {
			this.removeRepo(repo_name);
		}
		dht.peer().announceShutdown().start().awaitUninterruptibly();
		return true;
	}

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

}