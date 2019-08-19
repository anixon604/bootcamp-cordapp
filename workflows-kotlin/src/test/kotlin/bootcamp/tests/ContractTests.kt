package bootcamp.tests

import bootcamp.contracts.TokenContract
import bootcamp.states.TokenState
import net.corda.core.contracts.Contract
import net.corda.core.identity.CordaX500Name
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.DummyCommandData
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ContractTests {
    private val alice = TestIdentity(CordaX500Name("Alice", "", "GB")).party
    private val bob = TestIdentity(CordaX500Name("Bob", "", "GB")).party
    private val ledgerServices = MockServices(listOf("bootcamp"))

    private val tokenState = TokenState(alice, bob, 1)


    @Test
    fun `token contract implements contract`() {
        assert(TokenContract() is Contract)
    }

    @Test
    fun `token contract requires zero inputs in the transaction`() {
        ledgerServices.ledger {
            transaction {
                // has input with FAIL
                input(TokenContract.ID, tokenState)
                output(TokenContract.ID, tokenState)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                fails()
            }

            transaction {
                // has no input will verify
                output(TokenContract.ID, tokenState)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                verifies()
            }
        }
    }

    @Test
    fun `token contract requires on output in the transaction`() {
        ledgerServices.ledger {
            transaction {
                // has two outputs with FAIL
                output(TokenContract.ID, tokenState)
                output(TokenContract.ID, tokenState)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                fails()
            }

            transaction {
                // has one output will verify
                output(TokenContract.ID, tokenState)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                verifies()
            }
        }
    }

    @Test
    fun `token contract requires one command in the transaction`() {
        ledgerServices.ledger {
            transaction {
                // has two commands will fail
                output(TokenContract.ID, tokenState)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                fails()
            }

            transaction {
                // has one comand will verify
                output(TokenContract.ID, tokenState)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                verifies()
            }
        }
    }

    @Test
    fun `token contract requires the transaction output to be a token state`() {
        ledgerServices.ledger {
            transaction {
                // has wrong output will fail
                output(TokenContract.ID, DummyState())
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                fails()
            }

            transaction {
                // has correct output type, will verify
                output(TokenContract.ID, tokenState)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                verifies()
            }
        }
    }

    @Test
    fun `token contract requires the transaction output to have a positive number`() {
        val zeroTokenState = TokenState(alice, bob, 0)
        val negativeTokenState = TokenState(alice, bob, -1)
        val positiveTokenState = TokenState(alice, bob, 2)

        ledgerServices.ledger {
            transaction {
                // has zero-amount TokenState, will fail
                output(TokenContract.ID, zeroTokenState)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                fails()
            }

            transaction {
                // has negative-amount TokenState, will fail
                output(TokenContract.ID, negativeTokenState)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                fails()
            }

            transaction {
                // has positive-amount TokenState, will verify
                output(TokenContract.ID, tokenState)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                verifies()
            }

            transaction {
                // also has positive-amount TokenState, will verify
                output(TokenContract.ID, positiveTokenState)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                verifies()
            }
        }
    }

    @Test
    fun `token contract requires the transaction command to be an issue command`() {
        ledgerServices.ledger {
            transaction {
                // has wrong command type, will fail.
                output(TokenContract.ID, tokenState)
                command(listOf(alice.owningKey, bob.owningKey), DummyCommandData)
                fails()
            }

            transaction {
                // has correct command type, will verify
                output(TokenContract.ID, tokenState)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                verifies()
            }
        }
    }

    @Test
    fun `token contract requires issuer to be a required signer in the transaction`() {
        val tokenStateWhereBobIsIssuer = TokenState(bob, alice, 1)

        ledgerServices.ledger {
            transaction {
                // Issuer is not a required signer, will fail
                output(TokenContract.ID, tokenState)
                command(bob.owningKey, TokenContract.Commands.Issue())
                fails()
            }

            transaction {
                // Issuer is also not a required singer, will fail
                output(TokenContract.ID, tokenStateWhereBobIsIssuer)
                command(alice.owningKey, TokenContract.Commands.Issue())
                fails()
            }

            transaction {
                // Issuer is a required signer, will verify
                output(TokenContract.ID, tokenState)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                verifies()
            }

            transaction {
                // Issuer is also a required signer, will verify
                output(TokenContract.ID, tokenStateWhereBobIsIssuer)
                command(listOf(alice.owningKey, bob.owningKey), TokenContract.Commands.Issue())
                verifies()
            }
        }
    }
}