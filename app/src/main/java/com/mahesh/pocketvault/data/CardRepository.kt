package com.mahesh.pocketvault.data

class CardRepository(
    private val dao: CardDao,
    private val folderDao: FolderDao,
    private val groceryItemDao: GroceryItemDao,
    private val bankCardDao: BankCardDao
) {
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

    fun groceryItems(folderId: Long) = groceryItemDao.itemsByFolder(folderId)
    fun groceryCount(folderId: Long) = groceryItemDao.countByFolder(folderId)
    suspend fun addGroceryItem(item: GroceryItemEntity) = groceryItemDao.insert(item)
    suspend fun toggleGroceryItem(item: GroceryItemEntity) = groceryItemDao.update(item.copy(isDone = !item.isDone))
    suspend fun deleteGroceryItem(item: GroceryItemEntity) = groceryItemDao.delete(item)

    fun bankCards(folderId: Long) = bankCardDao.cardsByFolder(folderId)
    fun bankCardCount(folderId: Long) = bankCardDao.countByFolder(folderId)
    suspend fun addBankCard(card: BankCardEntity) = bankCardDao.insert(card)
    suspend fun markBankCardViewed(card: BankCardEntity) = bankCardDao.update(
        card.copy(usageCount = card.usageCount + 1, lastViewedAt = System.currentTimeMillis())
    )
    suspend fun deleteBankCard(card: BankCardEntity) = bankCardDao.delete(card)
}
