(ns v01.core
  (:require [carbon.vdom :as vdom]
            [v01.sync]                                      ; nothing from this ns is used here, but required to execute its code
            [v01.view :as view]))

(enable-console-print!)

(vdom/mount js/document.body [view/App])

