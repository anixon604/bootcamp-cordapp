package bootcamp.tests

import bootcamp.contracts.TokenContract
import bootcamp.flows.TokenIssueFlowInitiator
import bootcamp.flows.TokenIssueFlowResponder
import bootcamp.states.TokenState
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("bootcamp.contracts"),
        TestCordapp.findCordapp("bootcamp.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(TokenIssueFlowResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `transaction constructed by flows uses the correct notary`() {
        val flow = TokenIssueFlowInitiator(b.info.legalIdentities[0], 99)
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(1, signedTransaction.tx.outputStates.size)
        val output = signedTransaction.tx.outputs[0]

        assertEquals(network.notaryNodes[0].info.legalIdentities[0], output.notary)
    }

    @Test
    fun `transaction constructed by flow has one token state output with the correct amount and owner`() {
        val flow = TokenIssueFlowInitiator(b.info.legalIdentities[0], 99)
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(1, signedTransaction.tx.outputStates.size)
        val output = signedTransaction.tx.outputsOfType<TokenState>()[0]

        assertEquals(b.info.legalIdentities[0], output.owner)
        assertEquals(99, output.amount)
    }

    @Test
    fun `transaction constructed by flow has one output using the correct contract`() {
        val flow = TokenIssueFlowInitiator(b.info.legalIdentities[0], 99)
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(1, signedTransaction.tx.outputStates.size)
        val output = signedTransaction.tx.outputs[0]

        assertEquals("bootcamp.contracts.TokenContract", output.contract)
    }

    @Test
    fun `transaction constructed by flow has one issue command`() {
        val flow = TokenIssueFlowInitiator(b.info.legalIdentities[0], 99)
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(1, signedTransaction.tx.commands.size)
        val command = signedTransaction.tx.commands[0]

        assert(command.value is TokenContract.Commands.Issue)
    }

    @Test
    fun `transaction constructed by flow has one command with the issuer and the owner as signers`() {
        val flow = TokenIssueFlowInitiator(b.info.legalIdentities[0], 99)
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(1, signedTransaction.tx.commands.size)
        val command = signedTransaction.tx.commands[0]

        assertEquals(2, command.signers.size)
        assertTrue(command.signers.contains(a.info.legalIdentities[0].owningKey))
        assertTrue(command.signers.contains(b.info.legalIdentities[0].owningKey))
    }

    @Test
    fun `transaction constructed by flow has no inputs attachments or time windows`() {
        val flow = TokenIssueFlowInitiator(b.info.legalIdentities[0], 99)
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()

        assertEquals(0, signedTransaction.tx.inputs.size)
        // The single attachment is the contract attachment.
        assertEquals(1, signedTransaction.tx.attachments.size)
        assertNull(signedTransaction.tx.timeWindow)
    }
}