(ns duffel.fs-util)

(defn chroot-tree
    [root dir-tree]
    "Makes sure that the entire tree is chrooted to a directory. The given directory
    should not end in a /. If you want to chroot to root itself (/) pass in a blank
    string"
    (let [root-node  (first dir-tree)]
        (cons (assoc root-node :base-name root) (rest dir-tree))))

(defn append-slash [dir-name] (str dir-name "/"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; I couldn't explain again how these work, but they do. You give tree-map a
; function and a tree. The function should take three args, the first is a
; tree, the second the absolute prefix for this tree, and three the local
; duffel project prefix for this tree. The function should return the tree
; modified to your liking. When returned tree map will find all sub-trees
; (directories) in the tree you returned and run tree-map on them again, with
; aboslute and local prefixes updated accordingly. I'm leaving test-map around
; so you can see an example. It'll give each directory struct an :abs and a
; :local key so you can see what the list was being run with.
;
(declare _tree-map)
(defn tree-map
    [user-fn dir-tree]
    (let [ root-node  (first dir-tree)
           root-abs   (root-node :base-name)
           root-local "" ]
        (_tree-map user-fn (chroot-tree "" dir-tree) root-abs root-local)))

(defn _tree-map
    [user-fn dir-tree abs local]
    (let [ new-dir-tree      (user-fn dir-tree abs local)
           new-dir-tree-node (first new-dir-tree)
           new-dir-base-name (new-dir-tree-node :base-name)
           new-abs           (append-slash (str abs   (new-dir-tree-node :base-name)))
           new-local         (append-slash (str local (new-dir-tree-node :full-name))) ]
        (map #(if (seq? %)
                  (_tree-map user-fn % new-abs new-local)
                  %)
             new-dir-tree)))

(defn test-map [dir-tree abs-prefix local-prefix]
    (cons
        (assoc (first dir-tree) :abs   abs-prefix
                                :local local-prefix )
        (rest dir-tree)))
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn merge-meta
    "Given a file-struct and some metadata merges the file-struct's meta field
    with the given metadata"
    [file-struct meta-file-struct]
    (assoc file-struct :meta (merge (file-struct :meta {}) meta-file-struct)))

(defn merge-meta-reverse
    "Same as merge-meta, but whatever's already in the file-struct takes precedence"
    [file-struct meta-file-struct]
    (assoc file-struct :meta (merge meta-file-struct (file-struct :meta {}))))

(defn _merge-meta-dir
    "Given a dir-tree and metadata, merges the dir-tree's root element's metadata
    with the given metadata"
    [dir-tree meta-file-struct merge-fn]
    (cons (merge-fn (first dir-tree) meta-file-struct) (rest dir-tree)))

(defn merge-meta-dir         [d m] (_merge-meta-dir d m merge-meta))
(defn merge-meta-dir-reverse [d m] (_merge-meta-dir d m merge-meta-reverse))