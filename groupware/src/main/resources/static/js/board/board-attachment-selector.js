document.addEventListener("DOMContentLoaded", () => {

    const fileInput =
        document.getElementById("files");

    const selectedFileList =
        document.getElementById("selectedFileList");


    if (!fileInput || !selectedFileList) {
        return;
    }


    const form =
        fileInput.closest("form");

    const maxTotalFiles = 5;

    const maxFileSize =
        10 * 1024 * 1024;


    /*
     * 새로 선택한 파일
     */
    let selectedFiles = [];


    /*
     * 수정 화면에 존재하는 기존 파일
     *
     * 작성 화면에서는 0개
     */
    const existingRows =
        Array.from(
            document.querySelectorAll(
                ".board-existing-file-row"
            )
        );


    const existingDeleteButtons =
        document.querySelectorAll(
            ".board-existing-file-delete"
        );


    function getExistingCount() {

        return existingRows.filter(
            row =>
                !row.classList.contains(
                    "is-deleted"
                )
        ).length;
    }


    /*
     * 기존 첨부파일 삭제 예약
     *
     * 실제 삭제는 수정 버튼을 눌렀을 때 실행
     */
    existingDeleteButtons.forEach(
        button => {

            button.addEventListener(
                "click",
                () => {

                    const row =
                        button.closest(
                            ".board-existing-file-row"
                        );

                    const fileId =
                        button.dataset.fileId;


                    if (!row || !fileId || !form) {
                        return;
                    }


                    const hiddenId =
                        `delete-file-${fileId}`;

                    const alreadyDeleted =
                        row.classList.contains(
                            "is-deleted"
                        );


                    /*
                     * 삭제 취소
                     */
                    if (alreadyDeleted) {

                        /*
                         * 삭제 취소하면서 파일이 다시 살아났을 때
                         * 총 5개를 초과하면 복구 불가
                         */
                        if (
                            getExistingCount()
                            + 1
                            + selectedFiles.length
                            > maxTotalFiles
                        ) {

                            alert(
                                `첨부파일은 최대 ${maxTotalFiles}개까지 가능합니다.`
                            );

                            return;
                        }


                        row.classList.remove(
                            "is-deleted"
                        );

                        document
                            .getElementById(hiddenId)
                            ?.remove();

                        button.textContent =
                            "삭제";

                        return;
                    }


                    /*
                     * 삭제 예약
                     */
                    row.classList.add(
                        "is-deleted"
                    );


                    const hidden =
                        document.createElement(
                            "input"
                        );

                    hidden.type =
                        "hidden";

                    hidden.name =
                        "deleteFileIds";

                    hidden.value =
                        fileId;

                    hidden.id =
                        hiddenId;


                    form.appendChild(
                        hidden
                    );


                    button.textContent =
                        "삭제 취소";
                }
            );
        }
    );


    /*
     * 신규 파일 선택
     */
    fileInput.addEventListener(
        "change",
        () => {

            const newFiles =
                Array.from(
                    fileInput.files
                );


            /*
             * 이전에 선택했던 파일 +
             * 이번에 선택한 파일
             */
            const mergedFiles = [
                ...selectedFiles
            ];


            const oversizedFiles = [];


            newFiles.forEach(file => {

                /*
                 * 10MB 검사
                 */
                if (file.size > maxFileSize) {

                    oversizedFiles.push(
                        file.name
                    );

                    return;
                }


                /*
                 * 같은 파일 중복 선택 방지
                 */
                const duplicated =
                    mergedFiles.some(
                        savedFile =>
                            savedFile.name === file.name
                            && savedFile.size === file.size
                            && savedFile.lastModified === file.lastModified
                    );


                if (!duplicated) {

                    mergedFiles.push(
                        file
                    );
                }
            });


            if (oversizedFiles.length > 0) {

                alert(
                    "다음 파일은 10MB를 초과하여 추가할 수 없습니다.\n\n"
                    + oversizedFiles.join("\n")
                );
            }


            /*
             * 기존 + 신규 = 최대 5개
             */
            if (
                getExistingCount()
                + mergedFiles.length
                > maxTotalFiles
            ) {

                alert(
                    `첨부파일은 기존 파일을 포함하여 최대 ${maxTotalFiles}개까지 가능합니다.`
                );


                /*
                 * 방금 선택 전 상태로 복구
                 */
                syncInputFiles();

                return;
            }


            selectedFiles =
                mergedFiles;


            syncInputFiles();

            renderSelectedFiles();
        }
    );


    /*
     * input.files 재구성
     */
    function syncInputFiles() {

        const dataTransfer =
            new DataTransfer();


        selectedFiles.forEach(
            file => {

                dataTransfer.items.add(
                    file
                );
            }
        );


        fileInput.files =
            dataTransfer.files;
    }


    /*
     * 신규 선택 파일 리스트
     */
    function renderSelectedFiles() {

        selectedFileList.innerHTML =
            "";


        selectedFiles.forEach(
            (file, index) => {

                const row =
                    document.createElement(
                        "div"
                    );

                row.className =
                    "board-file-row";


                const info =
                    document.createElement(
                        "div"
                    );

                info.className =
                    "board-file-info";


                const icon =
                    document.createElement(
                        "span"
                    );

                icon.className =
                    "board-file-icon";

                icon.textContent =
                    "📎";


                const name =
                    document.createElement(
                        "span"
                    );

                name.className =
                    "board-file-name";

                name.textContent =
                    file.name;


                const size =
                    document.createElement(
                        "span"
                    );

                size.className =
                    "board-file-size";

                size.textContent =
                    formatFileSize(
                        file.size
                    );


                const removeButton =
                    document.createElement(
                        "button"
                    );

                removeButton.type =
                    "button";

                removeButton.className =
                    "board-file-remove";

                removeButton.textContent =
                    "취소";


                removeButton.addEventListener(
                    "click",
                    () => {

                        selectedFiles.splice(
                            index,
                            1
                        );


                        syncInputFiles();

                        renderSelectedFiles();
                    }
                );


                info.append(
                    icon,
                    name,
                    size
                );

                row.append(
                    info,
                    removeButton
                );

                selectedFileList.appendChild(
                    row
                );
            }
        );
    }


    function formatFileSize(bytes) {

        if (bytes < 1024) {
            return `${bytes} B`;
        }


        if (bytes < 1024 * 1024) {

            return `${(
                bytes / 1024
            ).toFixed(1)} KB`;
        }


        return `${(
            bytes / 1024 / 1024
        ).toFixed(1)} MB`;
    }
});