package br.mackenzie.ruralmed.repository;

import br.mackenzie.ruralmed.model.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CidRepository extends JpaRepository<Cid, String> {

    List<Cid> findTop20ByCodigoContainingIgnoreCaseOrDescricaoContainingIgnoreCase(String codigo, String descricao);
}
