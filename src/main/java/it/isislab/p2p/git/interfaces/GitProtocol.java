package it.isislab.p2p.git.interfaces;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import it.isislab.p2p.git.entity.Commit;
import it.isislab.p2p.git.entity.Item;
import it.isislab.p2p.git.entity.Repository;
import it.isislab.p2p.git.exceptions.ConflictsNotResolvedException;
import it.isislab.p2p.git.exceptions.GeneratedConflitException;
import it.isislab.p2p.git.exceptions.NothingToPushException;
import it.isislab.p2p.git.exceptions.RepoStateChangedException;
import it.isislab.p2p.git.exceptions.RepositoryAlreadyExistException;
import it.isislab.p2p.git.exceptions.RepositoryNotExistException;

public interface GitProtocol {

	/**
	 * @param repo_name una {@code String}, contenente il nome della repository.
	 * @return Un oggetto {@code Repository} contenente la repository salvata
	 *         localmente se esiste, {@code null} altrimenti.
	 */
	public Repository get_local_repo(String repo_name);

	/**
	 * @param repo_name una {@code String}, contenente il nome della repository.
	 * @return Un oggetto {@code Repository} contenente la repository salvata sulla
	 *         DHT se esiste, {@code null} altrimenti.
	 */
	public Repository get_remote_repo(String repo_name);

	/**
	 * @param repo_name una {@code String}, contenente il nome della repository.
	 * @return Un {@code ArrayList<Commit>} contenente i {@code Commit} non ancora
	 *         elaborati, {@code null} se non ne esistono.
	 */
	public ArrayList<Commit> get_local_commits(String repo_name);

	/**
	 * Crea una repository a partire da una directory aggiungendo tutti i file
	 * presenti, dopodichè clona la nuova repository nel path di destinazione.
	 * 
	 * @param repo_name una {@code String}, contenente il nome della repository.
	 * @param start_dir un {@code Path} contenente la posizione della directory da
	 *                  cui verra creata la repository.
	 * @param repo_dir  un {@code Path} dove verrà clonata la repository appena
	 *                  creata.
	 * @return {@code true} se creata correttamente, {@code false} negli altri casi.
	 * @throws RepositoryAlreadyExistException in caso si cerchi di creare una
	 *                                         repository già esistente.
	 */
	public boolean createRepository(String repo_name, Path start_dir, Path repo_dir) throws RepositoryAlreadyExistException;

	/**
	 * Clona una repository esistente.
	 * 
	 * @param repo_name una {@code String}, contenente il nome della repository.
	 * @param clone_dir un {@code Path} in cui verrà salvata la repository clonata.
	 * @return {@code true} se clonata correttamente, {@code false} negli altri
	 *         casi.
	 * @throws RepositoryNotExistException in caso si cerchi di creare una
	 *                                     repository inesistente.
	 */
	public boolean clone(String repo_name, Path clone_dir) throws RepositoryNotExistException;

	/**
	 * Aggiunge i file contenuti in una directory data alla repository.
	 * 
	 * @param repo_name una {@code String}, contenente il nome della repository.
	 * @param add_dir   un {@code Path} contente il file o i file da aggiungere.
	 * @return una {@code Collection<Item>} sei file sono stati aggiunti
	 *         correttamente, falso negli altri casi.
	 * @throws RepositoryNotExistException in caso si cerchi di aggiungere file a
	 *                                     una repository inesistente.
	 */
	public Collection<Item> addFilesToRepository(String repo_name, Path add_dir) throws RepositoryNotExistException;

	/**
	 * Crea un oggetto {@code Commit} con le modifiche effettuate e i file aggiunti.
	 * 
	 * @param repo_name una {@code String}, contenente il nome della repository.
	 * @param msg       una {@code String}, contenente il messaggio del commit.
	 * @return un oggetto {@code Commit} se sono state trovate delle modifiche,
	 *         {@code null} altrimenti.
	 */
	public Commit commit(String repo_name, String msg);

	/**
	 * Pusha tutti i commit in coda aggiornando lo stato della repository sulla DHT,
	 * inoltre se lo stato della repository è diverso da quello dell'ultimo pull, il
	 * push fallisce e chiede un pull.
	 * 
	 * @param repo_name una {@code String}, contenente il nome della repository.
	 * @return {@code true} se il push avviene correttamente, {@code false} negli
	 *         altri casi.
	 * @throws RepoStateChangedException   se lo stato della repository è cambiato
	 *                                     ed è richiesto un pull.
	 * @throws NothingToPushException      se non ci sono commit in coda da pushare.
	 * @throws RepositoryNotExistException se la repository su cui si sta cercando
	 *                                     di pushare non esiste.
	 */
	public Boolean push(String _repo_name) throws RepoStateChangedException, NothingToPushException, RepositoryNotExistException;

	/**
	 * Scarica i file dalla repository remota sulla DHT. Se ci sono conflitti, il
	 * file viene duplicato e il conflitto deve essere risolto manualmente per
	 * continuare.
	 * 
	 * @param repo_name una {@code String}, contenente il nome della repository.
	 * @return {@code true} se il pull avviene correttamente, {@code false} negli
	 *         altri casi.
	 * @throws RepositoryNotExistException se la repository su cui viene richiesto
	 *                                     il pull non esiste.
	 * @throws GeneratedConflitException   se il pull genera un conflitto.
	 * @throws ConflictsNotResolvedException        se i conflitti identificati non sono
	 *                                     stati risolti.
	 */
	public Boolean pull(String repo_name) throws RepositoryNotExistException, GeneratedConflitException, ConflictsNotResolvedException;

	/**
	 * Disconette il peer della DHT, eliminando tutte le repository e i file locali
	 * ad esse associati.
	 * 
	 * @param repo_name una {@code String}, contenente il nome della repository.
	 * @return {@code true} se il peeer viene disconesso correttamente, {@code false} negli
	 *         altri casi.
	 */
	public boolean leaveNetwork();

}