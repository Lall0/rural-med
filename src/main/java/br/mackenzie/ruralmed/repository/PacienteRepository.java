package br.mackenzie.ruralmed.repository;

import br.mackenzie.ruralmed.model.*;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    List<Paciente> findAllByOrderByNome();

    List<Paciente> findByMicroareaIdAndAtivoTrueOrderByNome(Long microareaId);

    Optional<Paciente> findByCpf(String cpf);

    Optional<Paciente> findByIdentificadorUnico(String identificadorUnico);
}
