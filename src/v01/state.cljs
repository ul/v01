(ns v01.state
  "Client app internal state. Some parts depend on synced control state, some don't."
  (:require [carbon.rx :as rx :include-macros true]
            [v01.control :as control]))

(defonce freq (rx/cursor control/state [:freq]))
