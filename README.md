# TempestGIT

|            Studente             |   Progetto   |
| :-----------------------------: | :----------: |
| **Giuseppe Arienzo 0522501062** | Git Protocol |

## Introduzione

Il progetto, implementa il protocollo **GIT** utilizzando una rete **P2P** basato su una **DHT**. Gli utenti possono:

- **Creare** una repository sulla rete;
- **Clonare** una repository preesistente sulla rete;
- **Aggiungere** file ad una repository;
- **Modificare** i file scaricati e caricare le modifiche sulla rete;

## Problem statement

Design and develop the **Git protocol**, distributed versioning control on a **P2P** network. Each peer can manage its projects (a set of files) using the **Git protocol** (a minimal version). The system allows the users to create a new repository in a specific folder, add new files to be tracked by the system, apply the changing on the local repository (commit function), push the network’s changes, and pull the changing from the network. The git protocol has lot-specific behavior to manage the conflicts; in this version, it is only required that if there are some conflicts, the systems can download the remote copy, and the merge is manually done. As described in the [GitProtocol Java API](https://github.com/spagnuolocarmine/distributedsystems-unisa/blob/master/homework/GitProtocol.java).

## Tecnologie utilizzate

- Vscode
- Apache Maven
- Tom P2P
- Java
- JUnit
- Docker
- HomeBrew

## Implementazione

Le funzionalità offerte dal protocollo sono quelle definite all'interno delle [GitProtocol Java API](https://github.com/spagnuolocarmine/distributedsystems-unisa/blob/master/homework/GitProtocol.java), opportunamente modificate in base alle necessità, sono inoltre state aggiunte ulteriori funzionalità come:

- La possibilità di **clonare** una repository;
- La possibilità di visualizzare lo **stato** di una repository, sia localmente che sulla rete.
- La possibilità di visualizzare tutti i **commit** in coda e pronti al push.

L'implementazione si basa su quattro classi principali:

- **Commit:** classe serializzabile che viene istanziata al momento della creazione di un commit e ne contiene messaggio e lista dei file (Item) aggiunti o modificati.
- **Generator:** implementa un metodo statico utilizzato per la generazione del **MD5** di un file a partire dal suo contenuto, tale MD5 viene utilizzato per alleggerire il processo di confronto dei file, limitandolo al confronto di due stringe.
- **Item:** classe serializzabile che rappresenta i file che vengono mantenuti all'interno della repository ne contiene infatti: nome, checksum e array di byte.
- **Repository:** classe serializzabile che rappresenta una repository mantenuta sulla rete, contiene un hashmap di: item, commit e peer (iscritti alla rete) nonché un identificativo che ne rappresenta la versione e un altro che ne rappresenta il proprietario. La classe implementa anche i seguenti metodi:
  - **Commit:** il quale dato in input un oggetto commit sincronizza lo stato della repository in base ad esso.
  - **isModified:** che permette, dato un file o un item, in ingresso di verificare a parità di nome, se il contenuto di quello presente nella repository è diverso.
  - **contains:** che permette di verificare se un determinato file esiste già nella repository.

Sono inoltre state definite una serie di [eccezioni](src/main/java/it/isislab/p2p/git/exceptions) che permettono la gestione di tutta una serie di errori che possono verificarsi durante l'esecuzione:

- **RepositoryNotExistException:** generata nel caso in cui si stia cercando di interagire con una repository inesistente.
- **RepositoryAlreadyExistException:** generata nel caso in cui si stia cercando di creare una repository che già esiste.
- **NothingToPushException** generata nel caso in cui si stia cercando di fare il push su una repository senza prima aver creato nessun commit.
- **RepoStateChangedException:** generata nel caso in cui lo stato della repository su cui si sta cercando di fare il push è cambiato ed è quindi necessario effettuare prima un pull.
- **GeneratedConflictException:** generata durante la fase di pull, nel caso in cui un file modificato localmente è stato modificato anche sulla repository remota.
- **ConflictsNotResolvedException:** generato durante la fase di pull, nel caso in cui non tutti i conflitti identificati siano stati risolti.

Le **API** aggiornate sono presenti [qui](src/main/java/it/isislab/p2p/git/interfaces/GitProtocol.java). E sono implementate all'interno del seguente [file](src/main/java/it/isislab/p2p/git/implementations/TempestGit.java) più nel dettaglio i principali metodi implementati sono:

### Create_Repository

Che si occupa della creazione di una nuova repository. Il metodo prima tenta di recuperare la repository che si sta cercando di creare dalla rete, se questa esiste viene lanciata una **RepositoryAlreadyExistException** nel caso contrario invece si procede alla creazione di un nuovo oggetto repository e all'iscrizione al topic del peer creante, infine la repository viene caricata sulla DHT e lanciato il metodo clone.

```Java
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

				// Clona la repository appena creata nella cartella di destinazione
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

```

### Clone

Il metodo permette di clonare una repository esistente sulla rete, tale metodo viene anche utilizzato per effettuare la sottoscrizione ad un topic, anche in questo caso per prima cosa si cerca di ottenere la repository richiesta dalla DHT, se la richiesta ha successo la repository viene scaricata localmente e il peer iscritto, in caso contrario viene lanciata l'eccezione **RepositoryNotExistException**.

```Java
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
					this.conflicts.put(repo_name, new ArrayList<String>());

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
```

### Add_File_To_Repository

Il metodo permette di aggiungere file a una repository, più nel dettaglio viene la directory data viene scansionata alla ricerca di file non contenuti nella repository locale, una volta identificati i tali file vengono inseriti all'interno di una HashMap in modo che possano essere inseriti nel successivo commit, per poi pusharli successivamente all'interno della repository.

```Java
  @Override
	public Collection<Item> addFilesToRepository(String repo_name, Path add_dir) throws RepositoryNotExistException {
		this.local_added.clear();

		if (this.local_repos.get(repo_name) != null) {

			File files[] = add_dir.toFile().listFiles();
			if (files != null) {
				for (File file : files) {
					if (!this.local_repos.get(repo_name).contains(file)) {
						try {
							this.local_added.put(file.getName(), new Item(file.getName(), Generator.md5_Of_File(file), Files.readAllBytes(file.toPath())));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

				return this.local_added.values();
			} else
				return null;
		} else {
			throw new RepositoryNotExistException();
		}
	}
```

### Commit

Il metodo permette di creare un commit, sulla base dei file modificati e dei file aggiunti.

```Java
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
```

### Push

Il metodo permette di fare il push di tutti i commit in coda localmente sulla repository, più nel dettaglio una volta ottenuta la repository remota presente sulla DHT (se non esiste si ritorna una **RepositoryNotExistException**) viene controllato se ci sono commit in coda, se non ci sono viene lanciata una **NothingToPushException** in caso contrario l'esecuzione procede e si controlla se la versione della repository locale è diverso da quello della repository remota, se cosi è vuol dire che dall'ultimo pull lo stato di quest'ultima e cambiata, viene quindi lanciata una **RepoStateChangedException** indicando che deve prima essere eseguito un pull, infine nel caso in cui tutte le condizioni sussistano all'operazione di pull si procede invocando il metodo **commit** della repository locale che ne aggiorna lo stato in base a ognuno dei commit in coda, infine la repository locale viene inserita nella DHT sovrascrivendo quella remota.

```Java
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
```

### Pull

```Java
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
```

#### Find_Conflict

```Java
private void find_Conflict(String repo_name, Repository remote_repo, HashMap<String, Item> modified, File[] local_files) throws GeneratedConflictException {
  Boolean find_conflit = false;

  for (Item item : modified.values()) {
    if (this.conflicts.get(repo_name) != null)
      // Se il file modificato in esame non è già stato identificato come conflitto
      if (!this.conflicts.get(repo_name).contains(item.getName()))
        // Ed è stato modificato anche in remoto
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

          this.conflicts.get(repo_name).add(item.getName());
          find_conflit = true;
        }
  }

  if (find_conflit)
    throw new GeneratedConflictException();
}
```

#### Update_Repo

```Java
private void update_repo(String repo_name, Repository remote_repo, HashMap<String, Item> modified) {
  this.local_repos.get(repo_name).setVersion(remote_repo.getVersion());

  for (Item item : remote_repo.getItems().values()) {
    // Se non c'è un conflitto su quell'item
    if (!this.conflicts.get(repo_name).contains(item.getName())) {

      // Se è già contenuto nella repository locale
      if (this.local_repos.get(repo_name).getItems().containsKey(item.getName())) {
        // Ed non è uno dei modificati
        if (!modified.containsKey(item.getName())) {
          this.local_repos.get(repo_name).getItems().get(item.getName()).setBytes(item.getBytes());
        }
      } else {
        this.local_repos.get(repo_name).getItems().put(item.getName(), item);
      }

      // Sovrascrivi o crei i file modificati o aggiunti
      File override = new File(this.my_repos.get(repo_name).toString(), item.getName());
      try {
        FileUtils.writeByteArrayToFile(override, item.getBytes());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
```

#### Check_Conflicts

```Java
private Boolean check_Conflicts(String repo_name) {
  if (this.conflicts.get(repo_name) != null)
    for (String file_name : this.conflicts.get(repo_name)) {
      File remote_version = new File(this.my_repos.get(repo_name).toString(), "/REMOTE-" + file_name);
      File local_version = new File(this.my_repos.get(repo_name).toString(), "/LOCAL-" + file_name);

      if (remote_version.exists() || local_version.exists()) {
        return false;
      }
    }
  return true;
}
```

Infine abbiamo la classe [Launcher](src/main/java/it/isislab/p2p/git/Launcher.java) che ho lo scopo di fungere da interfaccia e lanciare l'applicazione.

## Deployment

## Testing

## Problemi noti e future implementazioni
