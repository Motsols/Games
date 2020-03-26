import Vue from "vue";

import Socket from "../../socket";
import supportedGames from "@/supportedGames"

const gameStore = {
  namespaced: true,
  state: {
    actionPrevious: [],
    games: {}
  },
  getters: {},
  mutations: {
    createGame(state, data) {
      Vue.set(state.games, data.gameId, {
        gameInfo: {
          gameType: data.gameType,
          gameId: data.gameId,
          yourIndex: data.yourIndex,
          players: data.players
        },
        gameData: {
          eliminations: [],
          view: {},
          actions: {}
        }
      });
    },
    elimination(state, data) {
      let game = state.games[data.gameId].gameData;
      game.eliminations.push(data);
    },
    updateView(state, data) {
      let game = state.games[data.gameId].gameData;
      game.view = data.view
    },
    updateActions(state, data) {
      let game = state.games[data.gameId].gameData;
      let supportedGame = supportedGames.games[data.gameType]

      function resolveActionKey(actionName, type, value) {
        let actionKeys = supportedGame.actions[actionName]
        if (typeof actionKeys === 'object') {
          actionKeys = actionKeys[type]
        }
        return actionKeys(value)
      }

      let actions = {}
      data.actions.forEach(e => {
        let ca = {}
        let actionName = e.first
        actions[actionName] = ca

        console.log("ACTION INFO FOR", actionName, state.actionPrevious)
        let actionInfo = e.second
        console.log("ACTION INFO", actionInfo)
        actionInfo.nextOptions.forEach(next => {
          let key = resolveActionKey(actionName, "next", next)
          console.log("POSSIBLE NEXT", next, key)
          ca[key] = { next: next }
        })
        actionInfo.parameters.forEach(actionParam => {
          let key = resolveActionKey(actionName, "parameter", actionParam)
          console.log("POSSIBLE PARAM", actionParam, key)
          ca[key] = { parameter: actionParam }
        })
      })
      game.actions = actions
    }
  },
  actions: {
    action(context, data) {
      Socket.route(`games/${data.gameInfo.gameType}/${data.gameInfo.gameId}/move`, {
        moveType: data.name,
        move: data.data
      });
    },
    requestView(context, data) {
      Socket.route(`games/${data.gameType}/${data.gameId}/view`, {});
    },
    requestActions(context, data) {
      let game = context.state.games[data.gameInfo.gameId]
      if (!data.chosen) {
        data.chosen = [];
      }
      let obj = {
        playerIndex: game.gameInfo.yourIndex,
        chosen: data.chosen
      };
      if (data.actionType) { obj.moveType = data.actionType }

      Socket.route(`games/${data.gameInfo.gameType}/${data.gameInfo.gameId}/actionList`, obj);
    },
    onSocketMessage(context, data) {
      if (data.type === "GameStarted") {
        context.commit("createGame", data);
      }
      if (data.type == "PlayerEliminated") {
        context.commit("elimination", data);
      }
      if (data.type === "GameView") {
        context.commit("updateView", data);
      }
      if (data.type === "ActionList") {
        context.commit("updateActions", data);
      }
      if (data.type === "GameMove") {
        this.dispatch('DslGameState/requestView', data)
        this.dispatch('DslGameState/requestActions', { gameInfo: data })
      }
    }
  }
};

export default gameStore;
