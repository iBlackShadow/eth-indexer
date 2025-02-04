package io.stakenet.eth.explorer.repositories.transactions

import java.time.Instant
import java.time.temporal.ChronoUnit

import io.stakenet.eth.explorer.repository.blocks.BlocksPostgresRepository
import io.stakenet.eth.explorer.repository.transactions.TransactionsPostgresRepository
import io.stakenet.eth.explorer.{DatabaseSpec, Helpers}
import org.scalatest.wordspec.AnyWordSpec

class TransactionsPostgresRepositorySpec extends AnyWordSpec with DatabaseSpec {
  private lazy val blocksRepository = new BlocksPostgresRepository(database)
  private lazy val repository = new TransactionsPostgresRepository(database)

  "findByAddress" should {
    "find all transactions of an address" in {
      val address = Helpers.randomHash()
      val block = Helpers.randomBlock()
      val transaction1 = Helpers
        .randomTransaction()
        .copy(
          from = address,
          blockNumber = block.number,
          blockHash = block.hash,
          timestamp = block.timestamp
        )
      val transaction2 = Helpers
        .randomTransaction()
        .copy(
          to = Some(address),
          blockNumber = block.number,
          blockHash = block.hash,
          timestamp = block.timestamp
        )
      val transaction3 = Helpers.randomTransaction().copy(blockNumber = block.number, blockHash = block.hash)
      val transactions = List(transaction1, transaction2, transaction3)

      blocksRepository.create(block.withTransactions(transactions))

      val result = repository.findByAddress(address, limit = 100)

      result mustBe List(transaction1, transaction2).sortBy(_.hash)
    }

    "order transactions by time" in {
      val today = Instant.now
      val yesterday = today.minus(1, ChronoUnit.DAYS)
      val twoDaysAgo = today.minus(2, ChronoUnit.DAYS)
      val address = Helpers.randomHash()

      val block1 = Helpers.randomBlock().copy(timestamp = yesterday.toEpochMilli)
      val block2 = Helpers.randomBlock().copy(timestamp = today.toEpochMilli)
      val block3 = Helpers.randomBlock().copy(timestamp = twoDaysAgo.toEpochMilli)

      val transaction1 = Helpers
        .randomTransaction()
        .copy(
          from = address,
          blockNumber = block1.number,
          blockHash = block1.hash,
          timestamp = block1.timestamp
        )
      val transaction2 = Helpers
        .randomTransaction()
        .copy(
          to = Some(address),
          blockNumber = block2.number,
          blockHash = block2.hash,
          timestamp = block2.timestamp
        )
      val transaction3 = Helpers
        .randomTransaction()
        .copy(
          to = Some(address),
          blockNumber = block3.number,
          blockHash = block3.hash,
          timestamp = block3.timestamp
        )

      blocksRepository.create(block1.withTransactions(List(transaction1)))
      blocksRepository.create(block2.withTransactions(List(transaction2)))
      blocksRepository.create(block3.withTransactions(List(transaction3)))

      val result = repository.findByAddress(address, limit = 100)

      result mustBe List(transaction2, transaction1, transaction3)
    }

    "get an empty list when address has no transactions" in {
      val address = Helpers.randomHash()
      val block = Helpers.randomBlock()
      val transaction1 = Helpers
        .randomTransaction()
        .copy(
          from = address,
          blockNumber = block.number,
          blockHash = block.hash,
          timestamp = block.timestamp
        )
      val transaction2 = Helpers
        .randomTransaction()
        .copy(
          to = Some(address),
          blockNumber = block.number,
          blockHash = block.hash,
          timestamp = block.timestamp
        )
      val transaction3 = Helpers.randomTransaction().copy(blockNumber = block.number, blockHash = block.hash)
      val transactions = List(transaction1, transaction2, transaction3)

      blocksRepository.create(block.withTransactions(transactions))

      val result = repository.findByAddress(Helpers.randomHash(), limit = 100)

      result mustBe List()
    }

    "get token transfer transactions" in {
      val address = "0x0fb342955b20fa658e0cb7ff50902b7ea097b7fd"
      val block = Helpers.randomBlock()
      val transaction1 = Helpers
        .randomTransaction()
        .copy(
          blockNumber = block.number,
          blockHash = block.hash,
          timestamp = block.timestamp,
          input =
            "0xa9059cbb0000000000000000000000000fb342955b20fa658e0cb7ff50902b7ea097b7fd0000000000000000000000000000000000000000000000000000000004c4b400"
        )
      val transaction2 = Helpers.randomTransaction().copy(blockNumber = block.number, blockHash = block.hash)
      val transaction3 = Helpers.randomTransaction().copy(blockNumber = block.number, blockHash = block.hash)
      val transactions = List(transaction1, transaction2, transaction3)

      blocksRepository.create(block.withTransactions(transactions))

      val result = repository.findByAddress(address, limit = 100)

      result mustBe List(transaction1)
    }
  }
}
