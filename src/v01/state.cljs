(ns v01.state
  (:require [carbon.rx :as rx :include-macros true]
            [v01.control :as control]))

(defonce freq (rx/cursor control/state [:freq]))
