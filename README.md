# TempestGIT

<img src="logo.png" alt="logo"/>

|            Studente             |   Progetto   |
| :-----------------------------: | :----------: |
| **Giuseppe Arienzo 0522501062** | Git Protocol |
## Indice


  - [Introduzione](#introduzione)
  - [Problem statement](#problem-statement)
  - [Tecnologie utilizzate](#tecnologie-utilizzate)
  - [Implementazione](#implementazione)
  - [Deployment](#deployment)
  - [Testing](#testing)
  - [Problemi noti e considerazioni finali](#problemi-noti-e-considerazioni-finali)

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
- Bash Script

## Implementazione

Le funzionalità offerte dal protocollo sono quelle definite all'interno delle [GitProtocol Java API](https://github.com/spagnuolocarmine/distributedsystems-unisa/blob/master/homework/GitProtocol.java), opportunamente modificate in base alle necessità, sono inoltre state aggiunte ulteriori funzionalità come:

- La possibilità di **clonare** una `Repository`;
- La possibilità di visualizzare lo **stato** di una `Repository`, sia **localmente** che sulla **rete**.
- La possibilità di visualizzare tutti i **commit** in coda e pronti al **push**.

L'implementazione si basa su quattro classi principali:

- `Commit`: classe serializzabile che viene istanziata al momento della creazione di un **commit** e ne contiene messaggio e lista dei file (`Item`) aggiunti o modificati.
- `Generator`: implementa un metodo statico utilizzato per la generazione della **Checksum** (MD5) di un file a partire dal suo contenuto, tale **Checksum** viene utilizzato per alleggerire il processo di confronto dei file, limitandolo al confronto di due stringe.
- `Item`: classe serializzabile che rappresenta i file che vengono mantenuti all'interno della repository ne contiene infatti: **nome**, **checksum** e **array di byte**.
- `Repository`: classe serializzabile che rappresenta una **repository** mantenuta sulla rete, contiene un `hashmap` di: `Item`, `Commit` e `Peer` (iscritti alla rete) nonché un identificativo che ne rappresenta la versione e un altro che ne rappresenta il proprietario. La classe implementa anche i seguenti metodi:
  - **Commit:** il quale dato in input un oggetto `Commit` aggiorna lo stato della `Repository` in base ad esso.
  - **isModified:** che permette, dato un `File` o un `Item`, in input di verificare a parità di nome, se il contenuto di quello presente nella repository è diverso.
  - **contains:** che permette di verificare se un determinato `File` esiste già nella `Repository`.

Sono inoltre state definite una serie di [Exception](src/main/java/it/isislab/p2p/git/exceptions) che permettono la gestione di tutta una serie di errori che possono verificarsi durante l'esecuzione:

- `RepositoryNotExistException`: generata nel caso in cui si stia cercando di interagire con una `Repository` inesistente.
- `RepositoryAlreadyExistException`: generata nel caso in cui si stia cercando di creare una `Repository` che già esiste.
- `NothingToPushException`: generata nel caso in cui si stia cercando di fare il **push** su una `Repository` senza prima aver creato nessun **commit**.
- `RepoStateChangedException`: generata nel caso in cui lo **stato** della `Repository` su cui si sta cercando di fare il **push** è cambiato ed è quindi necessario effettuare prima un **pull**.
- `GeneratedConflictException`: generata durante la fase di **pull**, nel caso in cui un file modificato localmente è stato modificato anche sulla `Repository` remota.
- `ConflictsNotResolvedException`: generato durante la fase di **pull**, nel caso in cui non tutti i conflitti identificati siano stati risolti.

Le **API** aggiornate sono presenti nel file [GitProtocol](src/main/java/it/isislab/p2p/git/interfaces/GitProtocol.java). E sono implementate all'interno del file [TempestGit](src/main/java/it/isislab/p2p/git/implementations/TempestGit.java), più nel dettaglio i principali metodi implementati sono:

### Create_Repository

Che si occupa della creazione di una nuova `Repository`. Il metodo prima tenta di recuperare la `Repository` che si sta cercando di creare dalla rete, se questa esiste viene lanciata una `RepositoryAlreadyExistException` nel caso contrario invece si procede alla creazione di un nuovo oggetto `Repository` e all'iscrizione al topic del `Peer` creante, infine la `Repository` viene caricata sulla **DHT** e lanciato il metodo **clone**.

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

Il metodo permette di clonare una `Repository` esistente sulla rete, tale metodo viene anche utilizzato per effettuare la sottoscrizione ad un topic. Anche in questo caso per prima cosa si cerca di ottenere la `Repository` richiesta dalla **DHT**, se la richiesta ha successo la repository viene scaricata localmente e il peer iscritto, in caso contrario viene lanciata l'eccezione `RepositoryNotExistException`.

```Java
@Override
public boolean clone(String repo_name, Path clone_dir) throws RepositoryNotExistException {
  clone_dir = Path.of(this.work_dir.toString() + "/" + clone_dir.toString());

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

Il metodo permette di aggiungere file a una `Repository`, più nel dettaglio la directory data viene scansionata alla ricerca di **file** non contenuti nella **repository locale**, una volta identificati i tali **file** vengono inseriti all'interno di una `HashMap` in modo che possano essere inseriti nel successivo **commit**, per poi farne il **push** successivamente all'interno della **repository remota**.

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

Il metodo permette di creare un **commit**, sulla base dei file **modificati** e dei file **aggiunti**.

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

Il metodo permette di fare il push di tutti i `Commit` in coda localmente sulla `Repository`, più nel dettaglio una volta ottenuta la **repository remota** presente sulla **DHT** (se non esiste si ritorna una `RepositoryNotExistException`) viene controllato se ci sono `Commit` in coda, se non ci sono viene lanciata una `NothingToPushException` in caso contrario l'esecuzione procede e si controlla se la **versione** della **repository locale** è diversa da quello della **repository remota**, se cosi è vuol dire che dall'ultimo **pull** lo stato di quest'ultima e cambiata, viene quindi lanciata una `RepoStateChangedException` indicando che deve prima essere eseguito un **pull**, infine nel caso in cui tutte le condizioni sussistano all'operazione di **pull** si procede invocando il metodo `commit` della **repository locale** che ne aggiorna lo stato in base a ognuno dei `Commit` in coda, infine la **repository locale** viene inserita nella **DHT** sovrascrivendo quella **remota**.

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

Il metodo permette di fare il **pull** della **repository remota**, più nel dettaglio una volta ottenuta la **Repository remota** (`RepositoryNotExistException` in caso non esista) il metodo verifica se la **versione** della **repository locale** è diversa da quella **remota**, se cosi è vengono identificati tutti i file **modificati** e viene lanciato il metodo `Find_Conflict`.

Se lo stato della `Repository` non è invece cambiato si verifica se eventuali **conflitti** precedentemente identificati non sono stati risolti, se sono stati risolti si procede creando un `Commit` che contiene i conflitti risolti e si invoca il metodo `Update_Repo`, in caso contrario viene lanciata l'eccezione `ConflictsNotResolvedException`.

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

Si occupa di identificare i conflitti. Il metodo verifica, per ogni **file modificato** non già identificato come conflitto, se è stato modificato anche **sulla repository remota**, se cosi ne vengono create due copie, una identificata con la dicitura **REMOTE** e una con la dicitura **LOCALE**, in modo che l'utente posa scegliere quale mantenere. Infine se è stato identificato anche solo un conflitto viene generata una `GeneratedConflictException`.

```Java
private void find_Conflict(String repo_name, Repository remote_repo, HashMap<String, Item> modified, File[] local_files) throws GeneratedConflictException {
  Boolean find_conflict = false;

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
          find_conflict = true;
        }
  }

  if (find_conflict)
    throw new GeneratedConflictException();
}
```

#### Update_Repo

Si occupa di aggiornare i file locali in base allo stato della **repository locale** appena scaricata, più nel dettaglio il metodo scandisce i file presenti sulla **repository remota** su cui non sono stati identificati conflitti, a questo punto per ogni file, se modificato ne aggiorna il contenuto, se invece file sono file da aggiungere vengono aggiunti alla **repository locale**. Infine tutti i file contenuti nella **repository locale** vengono sovrascritti localmente, in modo da crearne eventuali nuovi e modificare gli altri.

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

Il metodo permette di verificare se tutti i **conflitti** identificati sono stati risolti andando a scandire la lista dei conflitti e verificando se ne esistono copie locali con la dicitura **REMOTE** o **LOCAL**.

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

Per semplificare la fase di deployment è stato realizzato uno [script bash](launch.sh) (Compatibile solo con MacOS ma facilmente adattabile a Linux ed eventualmente Windows) che predispone ed elimina, una volta terminato, un semplice ambiente per il testing dell'applicazione. Sono inoltre stati predisposti una serie di [file di test](src/test/resources), già inseriti come **defaultValue** di molte richieste di input, in questo modo è possibile testate tutte le funzionalità del programma senza copiare o creare file a mano.

```bash
git clone https://github.com/BeppeTemp/giuseppe-arienzo_adc_2021 && sh giuseppe-arienzo_adc_2021/launch.sh

```
In alternativa è possibile eseguire singolarmente i container:

### Master Peer

```bash
docker network create --subnet=172.20.0.0/16 Tempest-Net && docker run -i --net Tempest-Net --ip 172.20.128.0 -e MASTERIP="127.0.0.1" -e ID=0 --name Master-Peer beppetemp/tempest_git

```

### Generic Peer

```bash
docker run -i --net Tempest-Net -e MASTERIP="172.20.128.0" -e ID=1 --name Peer-One beppetemp/tempest_git

```

Nella generazione di numerosi **Generic Peer** è necessario iterare il parametro ID.

Inoltre nel caso si desideri collegarsi a uno dei container creati è possibile eseguire il seguente comando:

```bash
docker exec -t -i ${container_name} /bin/bash
```

## Testing

Per quanto riguarda la fase di **testing**, sono stati realizzati **31 test** con lo scopo di testare in modo approfondito i vari metodi implementati. Inoltre l'introduzione di un terzo parametro da linea di comando riguardante la `work_dir` di ogni `Peer`, permette di eseguire con semplicità più `Peer` sulla stessa macchina (impostando appunto `work_dir` differenti).

Inoltre è stato fornito uno **script** che permette di realizzare in pochi istanti una piccola rete, tramite l'utilizzo di container docker, tale script permette anche di fare il **binding** delle directory di lavoro dei container in modo da visualizzare comodamente cosa accade all'interno di essi.

## Problemi noti e considerazioni finali

L'applicazione è stata realizzata cercando di realizzare un protocollo che offrisse le stesse funzionalità del protocollo **GIT**, pertanto ogni `Peer` è virtualmente in grado di gestire più di una `Repository` contemporaneamente, anche se questa funzionalità non è stata testata in modo approfondito.

### Problemi noti

- Il metodo add non mostra i file aggiunti se prima non viene eseguito un pull (anche nel peer che li aggiunge).
