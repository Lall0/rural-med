package br.mackenzie.ruralmed.repository;

import br.mackenzie.ruralmed.model.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DossieRepository extends JpaRepository<Dossie, Long> {

    Optional<Dossie> findByIdLocal(UUID idLocal);

    List<Dossie> findAllByOrderByColetadoEmDesc();

    List<Dossie> findByStatusOrderByEscoreDesc(StatusDossie status);

    List<Dossie> findByAcsUsuarioIdOrderByColetadoEmDesc(Long acsId);

    List<Dossie> findByStatusAndEspecialidadeIdOrderByEscoreDescEncaminhadoEmAsc(StatusDossie status, Long especialidadeId);

    List<Dossie> findByPacienteIdOrderByColetadoEmAsc(Long pacienteId);

    List<Dossie> findByColetadoEmBetween(OffsetDateTime inicio, OffsetDateTime fim);
}
