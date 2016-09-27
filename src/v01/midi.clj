(ns v01.midi
  "Functions for handling MIDI event and controller input
  Pink's design  uses a virtual device system so that projects
  can be written to depend on the virtual device, and the
  real device can be configured per-system using a .pinkrc
  file. This allows both swapping in and out of hardware as
  well as creating mock hardware devices.
  Conventions are to use the following for virtual hardware
  names:
  * \"keyboard x\" - number of keyboard
  * \"knobs/sliders x\" - number of knobs/slider device
  Note, a hardware device can map to multiple virtual devices."
  (:require [pink.util :as util])
  (:import [java.util Arrays]
           [javax.sound.midi MidiSystem MidiDevice MidiDevice$Info
                             Receiver ShortMessage]
           [clojure.lang IFn]))

;; functions for listing registered MIDI devices

(defn- device-info->device
  "Parses out information about a MIDI connection from a MidiDevice$Info object
  and returns it and the MidiDevice it describes in a map."
  [^MidiDevice$Info info]
  {:name        (.getName info)
   :description (.getDescription info)
   :device-info info
   :device      (MidiSystem/getMidiDevice info)})

(defn list-devices []
  "Fetches list of available MIDI devices."
  (map device-info->device (MidiSystem/getMidiDeviceInfo)))

(defn input-device?
  "True when device can act as MIDI input. False otherwise."
  [{:keys [^MidiDevice device]}]
  (not (zero? (.getMaxTransmitters device))))

(defn output-device?
  "True when device can act as MIDI output. False otherwise."
  [{:keys [^MidiDevice device]}]
  (not (zero? (.getMaxReceivers device))))

(defn list-input-devices []
  "Lists all MIDI input devices."
  (filter input-device? (list-devices)))

(defn list-output-devices []
  "Lists all MIDI output devices."
  (filter output-device? (list-devices)))

;; Pink MIDI Manager

(defn create-manager []
  (atom {}))

;; processors set per channel
(defn add-virtual-device
  [midi-manager device-name]
  (let [vd {:name             device-name
            :keys             (boolean-array 128 false)
            :event-processors (make-array IFn 16)
            :cc-processors    (into-array (for [i (range 16)]
                                            (into-array
                                              (for [x (range 128)]
                                                (atom 0.0)))))
            :listener         nil
            :key-bindings     (atom {})}]
    (swap! midi-manager assoc device-name vd)
    vd))

(defn list-virtual-devices
  [midi-manager]
  @midi-manager)

(comment
  (let [f (create-manager)]
    (add-virtual-device f "slider/knobs 1")
    (add-virtual-device f "keyboard 1")
    (println (list-virtual-devices f))))

;; Binding

(defn device-is-named?
  "true when device-name is part of device's description or name."
  [^String device-name {:keys [^String description ^String name]}]
  (or (>= (.indexOf description device-name) 0)
      (>= (.indexOf name device-name) 0)))

(defn find-device
  "Finds device with device-name of device-type :in (input) or :out (output).
  Throws exception when multiple or zero matching devices are found."
  [device-name device-type]
  (let [found (filter (partial device-is-named? device-name)
                      ((device-type {:in  list-input-devices
                                     :out list-output-devices})))
        num-found (count found)]
    (cond
      (<= num-found 0)
      (throw (Exception. (str "No MIDI "
                              ({:in "input" :out "output"} device-type)
                              " devices found matching name: " device-name)))
      (> num-found 1)
      (let [names (map #(str "\t" (:name %) ": " (:description %) "\n") found)
            msg ^String (apply str "Multiple devices found (" num-found
                               ") matching name: " device-name "\n" names)]
        (throw (Exception. msg)))
      :else (first found))))

(defn create-receiver [virtual-device]
  (let [^"[[Lclojure.lang.Atom;" cc-processors
        (:cc-processors virtual-device)
        ^"[Lclojure.lang.IFn;" event-processors
        (:event-processors virtual-device)
        ]
    (reify Receiver
      (send [this msg timestamp]
        (when (instance? ShortMessage msg)
          (let [smsg ^ShortMessage msg
                cmd (.getCommand smsg)
                channel (.getChannel smsg)
                data1 (.getData1 smsg)
                data2 (.getData2 smsg)]
            (condp = cmd
              ShortMessage/CONTROL_CHANGE
              (when-let [atm (aget cc-processors channel data1)]
                (reset! atm data2))

              ShortMessage/NOTE_ON
              (when-let [efn (aget event-processors channel)]
                (efn cmd data1 data2))

              ShortMessage/NOTE_OFF
              (when-let [efn (aget event-processors channel)]
                (efn cmd data1 data2))
              ))

          )))))

(declare bind-key-func key-processor)

(defn bind-device
  [midi-manager ^String hardware-id ^String virtual-device-name]
  {:pre [midi-manager hardware-id virtual-device-name]}
  (println (format "Connecting %s to %s" hardware-id virtual-device-name))
  (let [device ^MidiDevice (:device (find-device hardware-id :in))
        virtual-device (@midi-manager virtual-device-name)]
    (when (nil? virtual-device)
      (throw (Exception. (format "Unknown virtual device: %s" virtual-device-name))))
    (when (not (.isOpen device))
      (.open device))
    (.setReceiver (.getTransmitter device)
                  (create-receiver virtual-device))
    ;; TODO move out into a separate function
    (bind-key-func virtual-device 0 (partial key-processor (:key-bindings virtual-device)))))

(defn bind-key-func
  [virtual-device ^long channel ^IFn afn]
  (aset ^"[Lclojure.lang.IFn;" (:event-processors virtual-device)
        channel afn))

(defn get-cc-atom
  [virtual-device channel cc-num]
  (aget (:cc-processors virtual-device)
        channel cc-num))

;(defn cc-trigger
;  [trigfn]
;  (fn [key atm old-v new-v]
;    (when (and (< old-v 127) (= new-v 127))
;      (trigfn)
;      )))

(defn set-event-processor
  [virtual-device channel midi-event-func]

  )

;; MIDI Device Debugging

(defn create-debug-receiver []
  (reify Receiver
    (send [this msg timestamp]
      (when (instance? ShortMessage msg)
        (let [smsg ^ShortMessage msg
              cmd (.getCommand smsg)
              channel (.getChannel smsg)
              data1 (.getData1 smsg)
              data2 (.getData2 smsg)]
          (println (format "%d %d %d %d" cmd channel data1 data2)))))))

(defn device-debug
  [^String hardware-id]
  (let [device ^MidiDevice (:device (find-device hardware-id :in))]
    (when (not (.isOpen device))
      (.open device))
    (.setReceiver (.getTransmitter device) (create-debug-receiver))
    ))


;; Utility functions


(defn midi->freq
  "Convert MIDI Note number to frequency in hertz"
  ^double [^long notenum]
  (* 440.0 (Math/pow 2.0 (/ (- notenum 57) 12))))

(defn midi->linear [x from to]
  (-> to (- from) (/ 127.0) (* x) (+ from)))

;; FIXME move to appropriate namespace
(defn atom->afn
  [source-atom transform]
  (let [out ^doubles (util/create-buffer)
        cur-val (atom @source-atom)]
    (fn []
      (let [v @source-atom]
        (when (not (= @cur-val v))
          (reset! cur-val v)
          (Arrays/fill out (double (transform v)))))
      out)))

(defn toggle-key [f virtual-device note-num on off]
  (swap! (:key-bindings virtual-device) update note-num
         (fn [m]
           (-> m
               (update :on (fnil f #{}) on)
               (update :off (fnil f #{}) off)))))

(def bind-key (partial toggle-key conj))
(def unbind-key (partial toggle-key disj))

(defn key-processor [key-bindings cmd note-num velocity]
  (condp = cmd
    ShortMessage/NOTE_ON
    (doseq [f (get-in @key-bindings [note-num :on])]
      (f velocity))

    ShortMessage/NOTE_OFF
    (doseq [f (get-in @key-bindings [note-num :off])]
      (f velocity))))

