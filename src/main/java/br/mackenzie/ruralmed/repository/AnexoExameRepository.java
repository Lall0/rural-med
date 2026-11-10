package br.mackenzie.ruralmed.repository;

import br.mackenzie.ruralmed.model.AnexoExame;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnexoExameRepository extends JpaRepository<AnexoExame, Long> {
    List<AnexoExame> findByDossieIdOrderByCriadoEmDesc(Long dossieId);
}
