document.addEventListener("DOMContentLoaded", () => {

    const fileInput =
        document.getElementById("signatureFile");

    const uploadButton =
        document.getElementById("signatureUploadButton");

    const deleteButton =
        document.getElementById("signatureDeleteButton");

    const deleteInput =
        document.getElementById("signatureDelete");

    const empty =
        document.getElementById("signatureEmpty");

    const previewArea =
        document.getElementById("signaturePreviewArea");

    const preview =
        document.getElementById("signaturePreview");


    // 해당 페이지에 사인 모듈이 없으면 종료
    if (!fileInput) {
        return;
    }


    let originalExists = false;
    let objectUrl = null;


    /*
     * DB에 실제 저장되어 있는 사인 조회
     */
    async function loadSignature() {

        clearObjectUrl();

        fileInput.value = "";
        deleteInput.value = "false";

        try {

            const response =
                await signatureRequest(
                    "/approval-signatures/api"
                );

            originalExists = response.exists;

            if (response.exists) {

                showSavedSignature();

            } else {

                showEmptySignature();
            }

        } catch (error) {

            console.error(error);
        }
    }


    /*
     * 현재 저장된 사인 표시
     */
    function showSavedSignature() {

        empty.style.display = "none";
        previewArea.style.display = "block";

        preview.src =
            "/approval-signatures/image?t="
            + Date.now();

        uploadButton.textContent =
            "사인 변경";

        deleteButton.style.display =
            "inline-block";
    }


    /*
     * 사인 없음 표시
     */
    function showEmptySignature() {

        empty.style.display = "flex";
        previewArea.style.display = "none";

        preview.removeAttribute("src");

        uploadButton.textContent =
            "사인 등록";

        deleteButton.style.display =
            "none";
    }


    /*
     * 파일 선택 버튼
     */
    uploadButton.addEventListener(
        "click",
        () => fileInput.click()
    );


    /*
     * 파일 선택
     *
     * 여기서는 서버에 저장하지 않고
     * 브라우저 미리보기만 변경한다.
     */
    fileInput.addEventListener(
        "change",
        () => {

            if (!fileInput.files.length) {
                return;
            }

            const file =
                fileInput.files[0];


            if (!file.type.startsWith("image/")) {

                alert("이미지 파일만 선택할 수 있습니다.");

                fileInput.value = "";

                return;
            }


            if (file.size > 5 * 1024 * 1024) {

                alert("이미지는 5MB 이하만 등록할 수 있습니다.");

                fileInput.value = "";

                return;
            }


            clearObjectUrl();

            objectUrl =
                URL.createObjectURL(file);


            preview.src =
                objectUrl;

            empty.style.display =
                "none";

            previewArea.style.display =
                "block";

            uploadButton.textContent =
                "사인 변경";

            deleteButton.style.display =
                "inline-block";


            /*
             * 새 파일을 선택했으므로
             * 삭제 예약 해제
             */
            deleteInput.value =
                "false";
        }
    );


    /*
     * 삭제 버튼
     *
     * 실제 DB/파일 삭제는 하지 않는다.
     * 삭제 예정 상태만 저장한다.
     */
    deleteButton.addEventListener(
        "click",
        () => {

            if (!confirm(
                "사인을 삭제하시겠습니까?\n저장 버튼을 눌러야 실제로 반영됩니다."
            )) {
                return;
            }


            clearObjectUrl();

            fileInput.value = "";


            /*
             * 원래 저장된 사인이 있었으면
             * 저장 시 삭제하도록 표시
             */
            deleteInput.value =
                originalExists
                    ? "true"
                    : "false";


            showEmptySignature();
        }
    );


    /*
     * 취소 등의 상황에서
     * 원래 서버 상태로 복원
     */
    window.resetSignatureEditor =
        function () {

            loadSignature();
        };


    function clearObjectUrl() {

        if (objectUrl) {

            URL.revokeObjectURL(
                objectUrl
            );

            objectUrl = null;
        }
    }


    loadSignature();
});


async function signatureRequest(
    url,
    options = {}) {

    const csrfToken =
        document.querySelector(
            'meta[name="_csrf"]'
        );

    const csrfHeader =
        document.querySelector(
            'meta[name="_csrf_header"]'
        );


    options.headers = {
        ...(options.headers || {})
    };


    if (csrfToken && csrfHeader) {

        options.headers[
            csrfHeader.content
            ] = csrfToken.content;
    }


    const response =
        await fetch(
            url,
            options
        );


    if (!response.ok) {

        const message =
            await response.text();

        throw new Error(
            message ||
            "요청에 실패했습니다."
        );
    }


    if (response.status === 204) {
        return null;
    }


    return response.json();
}