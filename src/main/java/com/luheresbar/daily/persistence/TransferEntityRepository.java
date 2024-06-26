package com.luheresbar.daily.persistence;

import com.luheresbar.daily.domain.Expense;
import com.luheresbar.daily.domain.Transfer;
import com.luheresbar.daily.domain.repository.ITransferRepository;
import com.luheresbar.daily.persistence.crud.ITransferCrudRepository;
import com.luheresbar.daily.persistence.entity.TransferEntity;
import com.luheresbar.daily.persistence.mapper.ITransferMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Repository
public class TransferEntityRepository implements ITransferRepository {

    private final ITransferCrudRepository transferCrudRepository;
    private final ITransferMapper transferMapper;

    public TransferEntityRepository(ITransferCrudRepository transferCrudRepository, ITransferMapper transferMapper) {
        this.transferCrudRepository = transferCrudRepository;
        this.transferMapper = transferMapper;
    }


    @Override //TODO (Varificar la necesidad de este metodo)
    public List<Transfer> getAll() {
        List<TransferEntity> transfers = transferCrudRepository.findAll();
        return transferMapper.toTransfers(transfers);
    }

//    @Override
//    public List<Transfer> getUserTransfers(Integer userId) {
//        // Metodo usando JdbcTemplate
//        String sql = "SELECT * FROM transfers WHERE user_id = ? ORDER BY transfer_date DESC";
//        List<TransferEntity> transferEntities = jdbcTemplate.query(sql, new Object[]{userId}, new BeanPropertyRowMapper<>(TransferEntity.class));
//        return this.transferMapper.toTransfers(transferEntities);
//    }

    @Override
    public List<Transfer> getUserTransfers(Integer userId) {
        List<TransferEntity> transferEntities = this.transferCrudRepository.findAllByUserIdOrderByTransferDate(userId);
        return this.transferMapper.toTransfers(transferEntities);
    }

    @Override
    public Optional<Transfer> getById(int transferId) {
        Optional<TransferEntity> transferEntity = this.transferCrudRepository.findById(transferId);
        return transferEntity.map(trn -> this.transferMapper.toTransfer(trn));
    }

    @Override
    public List<Transfer> findByDateBetween(LocalDateTime startDate, LocalDateTime endDate, int userId) {
        List<TransferEntity> transferEntities = this.transferCrudRepository.findByDateBetween(startDate, endDate, userId);

        return this.transferMapper.toTransfers(transferEntities);
    }

    @Override
    public Transfer save(Transfer transfer) {
        TransferEntity transferEntity = this.transferMapper.toTransferEntity(transfer);
        return this.transferMapper.toTransfer(this.transferCrudRepository.save(transferEntity));
    }

    @Override
    public boolean delete(int transferId, Integer userId) {
        TransferEntity transferEntity = this.transferCrudRepository.findById(transferId).orElse(null);
        if(transferEntity != null && transferEntity.getUserId().equals(userId)) {
            this.transferCrudRepository.delete(transferEntity);
            return true;
        }
        return false;
    }



}
