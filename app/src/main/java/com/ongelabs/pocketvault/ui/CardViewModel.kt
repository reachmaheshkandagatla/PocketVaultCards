package com.ongelabs.pocketvault.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ongelabs.pocketvault.data.AppDatabase
import com.ongelabs.pocketvault.data.BankCardEntity
import com.ongelabs.pocketvault.data.CardEntity
import com.ongelabs.pocketvault.data.CardRepository
import com.ongelabs.pocketvault.data.FolderEntity
import com.ongelabs.pocketvault.data.GroceryItemEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CardViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val repo = CardRepository(db.cardDao(), db.folderDao(), db.groceryItemDao(), db.bankCardDao())

    fun folders() = repo.folders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun cards(folderId: Long) = repo.cards(folderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun count(folderId: Long) = repo.count(folderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    fun deletedCards(folderId: Long) = repo.deletedCards(folderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun countDeleted(folderId: Long) = repo.countDeleted(folderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    fun groceryItems(folderId: Long) = repo.groceryItems(folderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun groceryCount(folderId: Long) = repo.groceryCount(folderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    fun bankCards(folderId: Long) = repo.bankCards(folderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun bankCardCount(folderId: Long) = repo.bankCardCount(folderId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addFolder(folder: FolderEntity) = viewModelScope.launch { repo.addFolder(folder) }
    fun renameFolder(folder: FolderEntity, name: String) = viewModelScope.launch { repo.renameFolder(folder, name.trim()) }
    fun deleteFolder(folder: FolderEntity) = viewModelScope.launch { repo.deleteFolder(folder) }

    fun add(card: CardEntity) = viewModelScope.launch { repo.add(card) }
    fun rename(card: CardEntity, name: String) = viewModelScope.launch { repo.rename(card, name.trim()) }
    fun markOpened(card: CardEntity) = viewModelScope.launch { repo.open(card) }
    fun togglePin(card: CardEntity) = viewModelScope.launch { repo.togglePin(card) }
    fun softDelete(card: CardEntity) = viewModelScope.launch { repo.softDelete(card) }
    fun restore(card: CardEntity) = viewModelScope.launch { repo.restore(card) }
    fun delete(card: CardEntity) = viewModelScope.launch { repo.delete(card) }

    fun addGroceryItem(item: GroceryItemEntity) = viewModelScope.launch { repo.addGroceryItem(item) }
    fun toggleGroceryItem(item: GroceryItemEntity) = viewModelScope.launch { repo.toggleGroceryItem(item) }
    fun deleteGroceryItem(item: GroceryItemEntity) = viewModelScope.launch { repo.deleteGroceryItem(item) }

    fun addBankCard(card: BankCardEntity) = viewModelScope.launch { repo.addBankCard(card) }
    fun markBankCardViewed(card: BankCardEntity) = viewModelScope.launch { repo.markBankCardViewed(card) }
    fun deleteBankCard(card: BankCardEntity) = viewModelScope.launch { repo.deleteBankCard(card) }
}
