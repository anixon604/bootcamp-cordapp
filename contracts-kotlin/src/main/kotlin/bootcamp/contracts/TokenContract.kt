package bootcamp.contracts

import bootcamp.states.TokenState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class TokenContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "bootcamp.contracts.TokenContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<TokenContract.Commands>(TokenContract.Commands::class.java)

        val inputs = tx.inputStates
        val outputs = tx.outputStates

        if (command.value is TokenContract.Commands.Issue) {
            requireThat {
                "Transaction must have no input states" using (inputs.isEmpty())
                "Transaction must have exactly one output" using (outputs.size == 1)
                "Output must be a TokenState" using (outputs[0] is TokenState)
                val output = outputs[0] as TokenState
                "Issuer must be required signer" using (command.signers.contains(output.issuer.owningKey))
                "Owner must be required signer" using (command.signers.contains(output.owner.owningKey))
                "Amount must be positive" using (output.amount > 0)
            }
        } else {
            throw IllegalArgumentException("Unrecognized command")
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
    }
}