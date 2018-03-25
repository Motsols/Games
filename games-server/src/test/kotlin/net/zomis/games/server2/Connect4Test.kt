package net.zomis.games.server2

import klogging.KLoggers
import net.zomis.games.server2.clients.ur.WSClient
import net.zomis.games.server2.clients.ur.getInt
import net.zomis.games.server2.clients.ur.getText
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI

class Connect4Test {

    private val logger = KLoggers.logger(this)

    var server: Server2? = null

    @BeforeEach
    fun startServer() {
        server = Server2(8378)
        server!!.start(arrayOf())
    }

    @AfterEach
    fun stopServer() {
        server!!.stop()
    }

    @Test
    fun connect4() {
        val p1 = WSClient(URI("ws://127.0.0.1:8378"))
        p1.connectBlocking()

        val p2 = WSClient(URI("ws://127.0.0.1:8378"))
        p2.connectBlocking()

//        p1.send("""v1:{ "game": "Connect2", "type": "matchMake" }""")
        p1.send("""v1:{ "game": "Connect4", "type": "matchMake" }""")
        Thread.sleep(100)
        p2.send("""v1:{ "game": "Connect4", "type": "matchMake" }""")
        p1.expectJsonObject {
            it.getText("type") == "GameStarted" && it.getText("gameType") == "Connect4" &&
                    it.getInt("yourIndex") == 0
        }
        p2.expectJsonObject {
            it.getText("type") == "GameStarted" && it.getText("gameType") == "Connect4" &&
                    it.getInt("yourIndex") == 1
        }

        sendAndExpect(p1, p2, listOf(
            Pair(1, 3), Pair(2, 4), Pair(1, 2), Pair(2, 5), Pair(1, 1), Pair(2, 6)
        ))

        // Try to cheat - wrong player
        p2.sendAndExpectResponse("""v1:{ "game": "Connect4", "gameId": "1", "type": "move", "move": 6 }""")
        p2.takeUntilJson { it.getText("type") == "IllegalMove" }

        // Win the game
        p1.sendAndExpectResponse("""v1:{ "game": "Connect4", "gameId": "1", "type": "move", "move": 0 }""")
        var obj = p1.takeUntilJson { it.getText("type") == "PlayerEliminated" }
        assert(obj.getText("gameType") == "Connect4")
        assert(obj.getInt("player") == 0)
        assert(obj.get("winner").asBoolean())
        assert(obj.getInt("position") == 1)

        obj = p1.expectJsonObject { it.getText("type") == "PlayerEliminated" }
        assert(obj.getText("gameType") == "Connect4")
        assert(obj.getInt("player") == 1)
        assert(!obj.get("winner").asBoolean())
        assert(obj.getInt("position") == 2)

        obj = p2.takeUntilJson { it.getText("type") == "PlayerEliminated" }
        assert(obj.getText("gameType") == "Connect4")
        assert(obj.getInt("player") == 0)
        assert(obj.get("winner").asBoolean())
        assert(obj.getInt("position") == 1)

        obj = p2.expectJsonObject { it.getText("type") == "PlayerEliminated" }
        assert(obj.getText("gameType") == "Connect4")
        assert(obj.getInt("player") == 1)
        assert(!obj.get("winner").asBoolean())
        assert(obj.getInt("position") == 2)

        p1.close()
        p2.close()
    }

    private fun sendAndExpect(p1: WSClient, p2: WSClient, pairs: List<Pair<Int, Int>>) {
        pairs.forEach({
            val cl = if (it.first == 1) p1 else p2
            cl.send("""v1:{ "game": "Connect4", "gameId": "1", "type": "move", "move": ${it.second} }""")
            p1.expectJsonObject { true }
            p2.expectJsonObject { true }
        })

//        p1.sendAndExpectResponse("""v1:{ "game": "Connect4", "gameId": "1", "type": "move", "move": 3 }""")
//        p2.sendAndExpectResponse("""v1:{ "game": "Connect4", "gameId": "1", "type": "move", "move": 4 }""")
//        p1.sendAndExpectResponse("""v1:{ "game": "Connect4", "gameId": "1", "type": "move", "move": 2 }""")
//        p2.sendAndExpectResponse("""v1:{ "game": "Connect4", "gameId": "1", "type": "move", "move": 5 }""")
//        p1.sendAndExpectResponse("""v1:{ "game": "Connect4", "gameId": "1", "type": "move", "move": 1 }""")
//        p2.sendAndExpectResponse("""v1:{ "game": "Connect4", "gameId": "1", "type": "move", "move": 6 }""")
    }

}
