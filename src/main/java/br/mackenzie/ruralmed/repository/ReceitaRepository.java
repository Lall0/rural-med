package br.mackenzie.ruralmed.repository;

import br.mackenzie.ruralmed.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceitaRepository extends JpaRepository<Receita, Long> {
}
