document.addEventListener(
    "DOMContentLoaded",
    () => {
        loadSignature().then(r => {
            const fileInput = document.getElementById("signatureFile");
            const uploadButton = document.getElementById("signatureUploadButton");
            const deleteButton = document.getElementById("signatureDeleteButton");

            uploadButton.addEventListener(
                "click",
                () => fileInput.click()
            );
            fileInput.addEventListener(
                "change",
                uploadSignature
            );

            deleteButton.addEventListener(
                "click",
                deleteSignature
            );
            }

        );
    }
);


async function loadSignature() {

    try {

        const response =
            await signatureRequest(
                "/approval-signatures/api"
            );


        const empty = document.getElementById("signatureEmpty");
        const previewArea = document.getElementById("signaturePreviewArea");
        const preview = document.getElementById( "signaturePreview");
        const uploadButton = document.getElementById("signatureUploadButton");
        const deleteButton =document.getElementById("signatureDeleteButton");

        if (!response.exists) {
            empty.style.display = "block";
            previewArea.style.display = "none";
            uploadButton.textContent = "사인 등록";
            deleteButton.style.display = "none";

            return;
        }


        empty.style.display = "none";
        previewArea.style.display = "block";

        /*
         * 브라우저 캐시 방지
         */
        preview.src =
            "/approval-signatures/image?t="
            + Date.now();


        uploadButton.textContent =
            "사인 변경";

        deleteButton.style.display =
            "inline-block";

    } catch (error) {

        console.error(error);
    }
}


async function uploadSignature() {

    const input =
        document.getElementById(
            "signatureFile"
        );


    if (!input.files.length) {
        return;
    }


    const formData =
        new FormData();

    formData.append(
        "file",
        input.files[0]
    );


    try {

        await signatureRequest(
            "/approval-signatures/api",
            {
                method: "POST",
                body: formData
            }
        );


        input.value = "";

        await loadSignature();

        alert("사인이 저장되었습니다.");

    } catch (error) {

        console.error(error);

        alert(
            error.message ||
            "사인 저장에 실패했습니다."
        );
    }
}


async function deleteSignature() {

    if (!confirm(
        "등록된 사인을 삭제하시겠습니까?"
    )) {
        return;
    }


    try {

        await signatureRequest(
            "/approval-signatures/api",
            {
                method: "DELETE"
            }
        );


        await loadSignature();

        alert("사인이 삭제되었습니다.");

    } catch (error) {

        console.error(error);

        alert(
            "사인 삭제에 실패했습니다."
        );
    }
}


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


    /*
     * FormData를 쓸 때는
     * Content-Type을 직접 지정하면 안 됨
     */
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