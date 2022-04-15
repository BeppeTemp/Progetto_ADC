# TempestGIT 

|            Studente             |   Progetto   |
| :-----------------------------: | :----------: |
| **Giuseppe Arienzo 0522501062** | Git Protocol |

## Introduzione

Il progetto, implementa il protocollo **GIT** utilizzando una rate **P2P** basato su una **DHT**. Gli utenti possono:
* **Creare** una repository sulla rete;
* **Clonare** una repository preesistente sulla rete;
* **Aggiungere** file ad una repository;
* **Modificare** i file scaricati e caricare le modifiche sulla rete;

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
* La possibilità di **clonare** una repository;
* La possibilità di visualizzare lo **stato** di una repository, sia locamente che sulla rete.
* La possibilità di visualizzare tutti i **commit** in coda e pronti al push.

L'implementazione si basa su quattro classi principali:
* **Commit:** classe serializzabile che viene istanziata al momento della creazione di un commit e ne contiene messaggio e lista dei file (Item) aggiunti o modificati.
* **Generator:** implementa un metodo statico utilizzato per la generazione del **MD5** di un file a partire dal suo contenuto, tale MD5 viene utilizzato per alleggerire il processo di confronto dei file, limitandolo al confronto di due stringe.
* **Item:** classe serializzabile che rappresenta i file che vengono mantenuti all'interno della repository ne contiene infatti: nome, checksum e array di byte.
* **Repository:** classe serializzabile che rappresenta una repository mantenuta sulla rete, contiene un hashmap di: item, commit e peer (iscritti alla rete) nonchè un identificativo che ne rappresenta la versione e un altro che ne rappresenta il proprietario. La classe implementa anche i seguenti metodi:
  * **Commit:** il quale dato in input un oggetto commit sincronizza lo stato della repository in base ad esso.
  * **inModified:** che permette, dato un file o un item, in ingresso di verificare a parità di nome, se il contenuto di quello presente nella repository è diverso.
  * **contains:** che permette di verificare se un determinato file esiste già nella repository. 

Sono inoltre state definite una serie di [eccezioni](src/main/java/it/isislab/p2p/git/exceptions) che permettono la gestione di tutta una serie di errori che possono verificarsi durante l'esecuzione:
* **RepositoryNotExistException:** generata nel caso in cui si stia cercando di interaggire con una reposity inesistente.
* **RepositoryAlreadyExistException:** generata nel caso in cui si stia cercando di creare una repository che già esiste.
* **NothingToPushException** generata nel caso in cui si stia cercando di fare il push su una repository senza prima aver creato nessun commit.
* **RepoStateChangedException:** generata nel caso in cui lo stato della repository su cui si sta cercando di fare il push è cambiato ed è quindi necessario effettuare prima un pull.
* **GeneratedConflitException:** generata nel caso in cui 
* **ConflictsNotResolvedException**


  
Le **API** aggiornate sono presenti [qui](src/main/java/it/isislab/p2p/git/interfaces/GitProtocol.java).
## Deployment

## Testing
## Problemi noti e future implementazioni