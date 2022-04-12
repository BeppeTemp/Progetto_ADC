package it.isislab.p2p.git.interfaces;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import it.isislab.p2p.git.entity.Commit;
import it.isislab.p2p.git.entity.Item;
import it.isislab.p2p.git.entity.Repository;
import it.isislab.p2p.git.exceptions.GeneratedConflitException;
import it.isislab.p2p.git.exceptions.NothingToPushException;
import it.isislab.p2p.git.exceptions.RepoStateChangedException;
import it.isislab.p2p.git.exceptions.RepositoryAlreadyExistException;
import it.isislab.p2p.git.exceptions.RepositoryNotExistException;

public interface GitProtocol {
	
	public Repository get_local_repo(String repo_name);
	public Repository get_remote_repo(String repo_name) throws Exception;
	public ArrayList<Commit> get_local_commits(String repo_name);
	
	/**
	 * ------------------ MODIFICATO DALLO STUDENTE ------------------
	 * Crea una repository a partire da una directory aggiungendo tutti i file presenti,
	 * dopodichè clona la nuova repository nel path di destinazione
	 * @param repo_name una Stringa, contenente il nome della repository.
	 * @param start_dir un Path contenente la posizione della directory da cui verra creata la repository.
	 * @param repo_dir un Path dove verrà clonata la repository appena creata.
	 * @return vero se creata correttamente, falso negli altri casi.
	 * @throws RepositoryAlreadyExistException in caso si cerchi di creare una reopsitory esistente
	 */
	public boolean createRepository(String repo_name, Path start_dir, Path repo_dir) throws RepositoryAlreadyExistException;
	
	/**
	 * ------------------ DEFINITO DALLO STUDENTE ------------------
	 * Clona una repository esistente
	 * @param repo_name una Stringa, contenente il nome della repository.
	 * @param clone_dir un Path in cui verrà salvata la repository clonata.
	 * @return vero se creata correttamente, falso negli altri casi.
	 * @throws Exception
	 */
	public boolean clone(String repo_name, Path clone_dir) throws Exception;
	
	/**
	 * ------------------ MODIFICATO DALLO STUDENTE ------------------
	 * @param repo_name una Stringa, contenente il nome della repository.
	 * @param add_dir un Path contente il file o i file da aggiungere.
	 * @return vero se creata correttamente, falso negli altri casi.
	 */
	public Collection<Item> addFilesToRepository(String _repo_name, Path add_dir);
	
	/**
	 * Applica i cambiamenti al file nella repository locale
	 * @param repo_name una Stringa, contenente il nome della repository.
	 * @param msg una Stringa, contenente il messaggio del commit.
	 * @return vero, se correttamente committato, falso negli altri casi.
	 */
	public Commit commit(String _repo_name, String msg);
	
	/**
	 * ------------------ MODIFICATO DALLO STUDENTE ------------------
	 * Pusha tutti i commit sulla rete, se lo stato della repository è diverso da quello 
	 * dell'ultimo pull, il push fallisce e chiede un pull.
	 * @param repo_name una Stringa, contenente il nome della repository.
	 * @return una Stringa, contenten un messaggi operativo.
	 * @throws RepoStateChangedException
	 * @throws NothingToPushException
	 * @throws RepositoryNotExistException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public Boolean push(String _repo_name) throws RepoStateChangedException, NothingToPushException, RepositoryNotExistException;
	
	/**
	 * Pull the files from the Network. If there is a conflict, the system duplicates 
	 * the files and the user should manually fix the conflict.
	 * @param _repo_name _repo_name a String, the name of the repository.
	 * @return a String, operation message.
	 * @throws RepositoryNotExistException
	 * @throws GeneratedConflitException
	 */
	public Boolean pull(String _repo_name) throws RepositoryNotExistException, GeneratedConflitException;

	/**
	 * ------------------ DEFINED BY STUDENT ------------------
	 * Clone an existing repository.
	 * @param repo_name una Stringa, contenente il nome della repository.
	 * @return true if it is correctly created, false otherwise.
	 */
	public boolean leaveNetwork();

}