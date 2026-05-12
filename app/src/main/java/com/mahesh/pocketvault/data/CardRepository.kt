package com.mahesh.pocketvault.data

class CardRepository(private val dao: CardDao, private val folderDao: FolderDao) {
    fun folders() = folderDao.allFolders()
    suspend fun addFolder(folder: FolderEntity) = folderDao.insert(folder)
    suspend fun deleteFolder(folder: FolderEntity) = folderDao.delete(folder)
    suspend fun getFolder(id: Long) = folderDao.getById(id)

    fun cards(folderId: Long) = dao.cardsByFolder(folderId)
    fun count(folderId: Long) = dao.countByFolder(folderId)
    fun deletedCards(folderId: Long) = dao.deletedCardsByFolder(folderId)
    fun countDeleted(folderId: Long) = dao.countDeletedByFolder(folderId)
    
    suspend fun add(card: CardEntity) = dao.insert(card)
    suspend fun open(card: CardEntity) = dao.update(card.copy(usageCount = card.usageCount + 1, lastOpenedAt = System.currentTimeMillis()))
    suspend fun togglePin(card: CardEntity) = dao.update(card.copy(isPinned = !card.isPinned))
    suspend fun softDelete(card: CardEntity) = dao.update(card.copy(isDeleted = true, deletedAt = System.currentTimeMillis()))
    suspend fun restore(card: CardEntity) = dao.update(card.copy(isDeleted = false, deletedAt = null))
    suspend fun delete(card: CardEntity) = dao.delete(card)
    suspend fun get(id: Long) = dao.getById(id)
}
