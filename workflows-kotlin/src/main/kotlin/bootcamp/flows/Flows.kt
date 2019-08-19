package bootcamp.flows

import bootcamp.contracts.TokenContract
import bootcamp.states.TokenState
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class TokenIssueFlowInitiator(val owner: Party, val amount: Int) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() : SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val issuer = ourIdentity

        val tokenState = TokenState(issuer, owner, amount)

        // build the transaction
        var txBuilder = TransactionBuilder(notary)
        val commandData = TokenContract.Commands.Issue()
        txBuilder.addCommand(commandData, issuer.owningKey, owner.owningKey)
        txBuilder.addOutputState(tokenState, TokenContract.ID)
        txBuilder.verify(serviceHub)

        // initiate the flow
        val session = initiateFlow(owner)

        val signedTransaction = serviceHub.signInitialTransaction(txBuilder)
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(signedTransaction, listOf(session)))

        return subFlow(FinalityFlow(fullySignedTransaction, listOf(session)))
    }
}

@InitiatedBy(TokenIssueFlowInitiator::class)
class TokenIssueFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // implement responder flow transaction checks here
            }
        }
        val txId = subFlow(signTransactionFlow).id

        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}
