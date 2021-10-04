package gk.kademlia

/**
 *
https://github.com/ep2p/kademlia-api
 */
enum class AgentApiKey {

    /**
     * https://youtu.be/7o0pfKDq9KE?t=252
    giveMeAListOfNodesThatArecloseToMe  */
    routesUpdate,

    /**
     * https://youtu.be/7o0pfKDq9KE?t=355
     */
    ping,

    /**
     * https://youtu.be/7o0pfKDq9KE?t=1010
     */
    copyDataToOtherNodes,

    /**
     * https://youtu.be/7o0pfKDq9KE?t=1010
     */
    moveDataToClosestNodesDuringShutdown,

    /**
     * https://youtu.be/7o0pfKDq9KE?t=1010
     */
    askClosestNodesForDataToStore,

    /**
     * node x joins this agent.
     * this agent sends an unused guid or error message.
     *
     *
     */
    join
}