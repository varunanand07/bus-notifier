. .env
transpose() {
    # script to deduce the order that would be given by spring boot
    script=$(head -n1 $1 | perl -F, -e '$o = join "\",\"", map {"\$".($_->[0]+1)} sort {$a->[1] cmp $b->[1]} map {[$_, $F[$_]]} (keys @F) ; print "{ print $o }"')
    awk -F , -v RS='\r\n' "$script" $1
}
mkdir -p transposed
for file in "$GTFS_DIR"/*.txt
do
    [[ "$file" =~ "shapes.txt" ]] && continue;
    echo transposing "$(basename $file)"
    transpose "$file" > transposed/"$(basename $file)"
done
