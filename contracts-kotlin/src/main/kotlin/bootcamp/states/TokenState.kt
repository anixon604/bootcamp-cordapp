package bootcamp.states

import bootcamp.contracts.TokenContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(TokenContract::class)
data class TokenState(val issuer: Party, val owner: Party, val amount: Int, override val participants: List<AbstractParty> = listOf(issuer, owner)) : ContractState
