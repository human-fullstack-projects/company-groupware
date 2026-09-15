function setupAttachmentSelector({
                                     inputId = "documentFile",
                                     listId = "selectedFileList",
                                     existingCount = 0,
                                     maxTotalFiles = 5,
                                     maxFileSize = 10 * 1024 * 1024
                                 } = {}) {

    const fileInput =
        document.getElementById(inputId);

    const fileList =
        document.getElementById(listId);


    if (!fileInput || !fileList) {
        return;
    }


    /*
     * 새로 선택한 파일들을 따로 관리
     */
    let selectedFiles = [];


    /*
     * 현재 추가 가능한 파일 개수
     *
     * ex)
     * 기존 파일 2개
     * 최대 5개
     * → 신규 선택 최대 3개
     */
    const maxNewFiles =
        Math.max(
            maxTotalFiles - existingCount,
            0
        );


    /*
     * 파일 선택
     */
    fileInput.addEventListener(
        "change",
        () => {

            const newFiles =
                Array.from(
                    fileInput.files
                );


            /*
             * input.files에는 방금 선택한 파일만 들어오므로
             * 기존 selectedFiles와 합침
             */
            const mergedFiles = [
                ...selectedFiles
            ];


            const oversizedFiles = [];


            newFiles.forEach(file => {

                /*
                 * 파일 크기 검사
                 */
                if (file.size > maxFileSize) {

                    oversizedFiles.push(
                        file.name
                    );

                    return;
                }


                /*
                 * 동일 파일 중복 선택 방지
                 */
                const duplicated =
                    mergedFiles.some(
                        savedFile =>
                            savedFile.name === file.name
                            && savedFile.size === file.size
                            && savedFile.lastModified === file.lastModified
                    );


                if (!duplicated) {
                    mergedFiles.push(file);
                }
            });


            /*
             * 10MB 초과 파일 안내
             */
            if (oversizedFiles.length > 0) {

                alert(
                    "다음 파일은 10MB를 초과하여 추가할 수 없습니다.\n\n"
                    + oversizedFiles.join("\n")
                );
            }


            /*
             * 기존 파일 + 신규 파일 합계 검사
             */
            if (mergedFiles.length > maxNewFiles) {

                alert(
                    `첨부파일은 기존 파일을 포함하여 최대 ${maxTotalFiles}개까지 가능합니다.\n`
                    + `현재 새로 추가할 수 있는 파일은 ${maxNewFiles}개입니다.`
                );


                /*
                 * 기존 선택 상태 복구
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
     * input.files 다시 생성
     *
     * input.files는 직접 splice 등이 안 되기 때문에
     * DataTransfer를 이용해서 다시 만들어줌
     */
    function syncInputFiles() {

        const dataTransfer =
            new DataTransfer();


        selectedFiles.forEach(file => {

            dataTransfer.items.add(file);

        });


        fileInput.files =
            dataTransfer.files;
    }


    /*
     * 선택 파일 목록 출력
     */
    function renderSelectedFiles() {

        fileList.innerHTML = "";


        if (selectedFiles.length === 0) {
            return;
        }


        selectedFiles.forEach(
            (file, index) => {

                const row =
                    document.createElement("div");

                row.className =
                    "selected-file-row";


                /*
                 * 왼쪽 파일정보
                 */
                const info =
                    document.createElement("div");

                info.className =
                    "selected-file-info";


                const icon =
                    document.createElement("span");

                icon.className =
                    "attachment-icon";

                icon.textContent =
                    "📎";


                const name =
                    document.createElement("span");

                name.className =
                    "selected-file-name";

                name.textContent =
                    file.name;


                const size =
                    document.createElement("span");

                size.className =
                    "selected-file-size";

                size.textContent =
                    formatFileSize(
                        file.size
                    );


                info.appendChild(icon);
                info.appendChild(name);
                info.appendChild(size);


                /*
                 * 취소 버튼
                 */
                const removeButton =
                    document.createElement("button");

                removeButton.type =
                    "button";

                removeButton.className =
                    "btn selected-file-remove";

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


                row.appendChild(info);
                row.appendChild(
                    removeButton
                );


                fileList.appendChild(row);
            }
        );
    }


    /*
     * 파일 크기 표시
     */
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
            bytes
            / 1024
            / 1024
        ).toFixed(1)} MB`;
    }
}