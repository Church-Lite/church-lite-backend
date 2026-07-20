package com.smartverse.churchlitebackend.service.cells;

import com.potatotech.authorization.exception.ServiceException;
import com.smartverse.churchlitebackend_gen.dtos.CellOrganizationUnitDTO;
import com.smartverse.churchlitebackend_gen.services.CellOrganizationUnitService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.UUID;

@Service
public class CellOrganizationUnitBusinessService extends CellOrganizationUnitService {
    @Override
    @Transactional
    public CellOrganizationUnitDTO update(CellOrganizationUnitDTO dto, UUID id) {
        validateParent(dto, id);
        var current = repository.findById(id).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "Unidade organizacional não encontrada"));
        var entity = dtoConverter.toEntity(dto, null);
        entity.setId(id);
        entity.setChildren(current.getChildren());
        return dtoConverter.toDTO(repository.save(entity), null);
    }

    @Override
    @Transactional
    public CellOrganizationUnitDTO save(CellOrganizationUnitDTO dto) {
        validateParent(dto, null);
        return super.save(dto);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        var unit = repository.findById(id).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "Unidade organizacional não encontrada"));
        if (unit.getChildren() != null && !unit.getChildren().isEmpty())
            throw new ServiceException(HttpStatus.CONFLICT, "A unidade possui subunidades e não pode ser excluída");
        super.delete(id);
    }

    private void validateParent(CellOrganizationUnitDTO dto, UUID id) {
        if (dto.getName() == null || dto.getName().isBlank() || dto.getLevelType() == null)
            throw new ServiceException(HttpStatus.BAD_REQUEST, "Nome e tipo do nível são obrigatórios");
        if (dto.getParentUnit() == null || dto.getParentUnit().getId() == null) return;
        UUID cursor = dto.getParentUnit().getId();
        var visited = new HashSet<UUID>();
        while (cursor != null) {
            if (cursor.equals(id) || !visited.add(cursor))
                throw new ServiceException(HttpStatus.CONFLICT, "A hierarquia não pode possuir ciclos");
            var parent = repository.findById(cursor).orElseThrow(() -> new ServiceException(HttpStatus.BAD_REQUEST, "Unidade superior inválida"));
            cursor = parent.getParentUnit() == null ? null : parent.getParentUnit().getId();
        }
    }
}
