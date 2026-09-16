const saveButton = document.getElementById("saveButton");
const deleteButton = document.getElementById("deleteButton");
const approveButton = document.getElementById("approveButton");
const rejectButton = document.getElementById("rejectButton");

const rejectModal = document.getElementById("rejectModal");
const rejectForm = document.getElementById("rejectForm");
const rejectComment = document.getElementById("rejectComment");
const rejectCancelButton = document.getElementById("rejectCancelButton");
const rejectError = document.getElementById("rejectError");

const approvalPage = document.getElementById("approvalPage");//문서 아이디 저장해둔 위치
const documentId = Number(approvalPage.dataset.documentId);


const attachmentInput =
    document.getElementById(
        "documentFile"
    );


if (attachmentInput) {
    setupAttachmentSelector({
        inputId: "documentFile",
        listId: "selectedFileList",
        existingCount: Number(attachmentInput.dataset.existingCount || 0),
        maxTotalFiles: 5
    });
}


const attachementDeleteButton = document.querySelectorAll(".attachment-delete");


// 수정
saveButton?.addEventListener("click", async () => {
        const title = document.getElementById("documentTitle").value.trim();
        const content = document.getElementById("documentContent").value.trim();

        if (!title) {
            alert("제목을 입력해주세요.");
            return;
        }

        if (!content) {
            alert("내용을 입력해주세요.");
            return;
        }

        await request(
            `/approvals/api/${documentId}`,
            {
                method: "PUT",
                body: JSON.stringify({
                    title,
                    content,
                    approvalLineId: null
                })
            }
        );

        try {
            await uploadAttachments(
                documentId
            );
        } catch (fileError) {

            console.error(fileError);

            alert(
                "문서 내용은 수정되었지만\n" +
                "첨부파일 추가에 실패했습니다."
                // fileError.message
            );

            location.reload();
            return;
        }


        alert("수정되었습니다.");

        location.reload();

    }
);


// 삭제
deleteButton?.addEventListener(
    "click",
    async () => {

        if (!confirm("문서를 삭제하시겠습니까?")) {
            return;
        }

        await request(`/approvals/api/${documentId}`, {method: "DELETE"});

        alert("삭제되었습니다.");

        location.href = "/approvals";
    }
);


// 승인
approveButton?.addEventListener(
    "click",
    async () => {
        if (!confirm("이 문서를 승인하시겠습니까?")) {
            return;
        }

        await request(
            `/approvals/api/${documentId}/approve`,
            {
                method: "POST",
                body: JSON.stringify({comment: null})
            }
        );

        alert("승인되었습니다.");

        location.href = "/approvals";
    }
);


// 반려
// 반려 모달 열기
rejectButton?.addEventListener("click", () => {

    rejectComment.value = "";
    rejectError.textContent = "";

    rejectModal.classList.add("is-open");

    rejectComment.focus();
});


// 반려 모달 닫기
function closeRejectModal() {

    rejectModal?.classList.remove("is-open");

    if (rejectComment) {
        rejectComment.value = "";
    }

    if (rejectError) {
        rejectError.textContent = "";
    }
}


// 취소 버튼
rejectCancelButton?.addEventListener(
    "click",
    closeRejectModal
);


// 모달 바깥 영역 클릭 시 닫기
rejectModal?.addEventListener(
    "click",
    (event) => {

        if (event.target === rejectModal) {
            closeRejectModal();
        }
    }
);


// ESC 키로 닫기
document.addEventListener(
    "keydown",
    (event) => {

        if (
            event.key === "Escape"
            && rejectModal?.classList.contains("is-open")
        ) {
            closeRejectModal();
        }
    }
);


// 반려 실행
rejectForm?.addEventListener(
    "submit",
    async (event) => {

        event.preventDefault();

        const comment =
            rejectComment.value.trim();


        // 반려 사유 미입력
        if (!comment) {

            rejectError.textContent =
                "반려 사유를 입력해주세요.";

            rejectComment.focus();

            return;
        }


        rejectError.textContent = "";


        try {

            await request(
                `/approvals/api/${documentId}/reject`,
                {
                    method: "POST",
                    body: JSON.stringify({
                        comment: comment
                    })
                }
            );


            alert("반려되었습니다.");

            location.href = "/approvals";

        } catch (error) {

            console.error(error);
        }
    }
);

// rejectButton?.addEventListener("click", async () => {
//         const comment = prompt("반려 사유를 입력해주세요.");
//         if (comment === null) {
//             return;
//         }
//
//         await request(
//             `/approvals/api/${documentId}/reject`,
//             {
//                 method: "POST",
//                 body: JSON.stringify({comment: comment})
//             }
//         );
//
//         alert("반려되었습니다.");
//         location.href = "/approvals";
//     }
// );


// 첨부파일 삭제
attachementDeleteButton.forEach(button => {
    button.addEventListener("click", async () => {
            const attachmentId =
                Number(
                    button.dataset.attachmentId
                );

            if (!confirm(
                "이 첨부파일을 삭제하시겠습니까?"
            )) {
                return;
            }


            try {
                await request(
                    `/approvals/api/${documentId}/attachments/${attachmentId}`,
                    {
                        method: "DELETE"
                    }
                );


                alert(
                    "첨부파일이 삭제되었습니다."
                );

                location.reload();

            } catch (error) {

                console.error(error);
            }
        }
    );
});


// 요청 처리용 함수
async function request(url, options = {}) {
    const csrfToken = document.querySelector('meta[name="_csrf"]');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]');


    options.headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };


    if (csrfToken && csrfHeader) {
        options.headers[csrfHeader.content] = csrfToken.content;
    }

    const response = await fetch(url, options);

    if (!response.ok) {
        const message = await response.text();
        console.error(message);
        alert("요청 처리 중 오류가 발생했습니다.");
        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    return await response.json();
}

async function uploadAttachments(
    documentId) {

    const fileInput =
        document.getElementById(
            "documentFile"
        );


    if (!fileInput
        || !fileInput.files.length) {

        return;
    }


    const files =
        Array.from(
            fileInput.files
        );


    if (files.length > 5) {

        throw new Error(
            "첨부파일은 최대 5개까지 가능합니다."
        );
    }


    for (const file of files) {

        if (file.size >
            10 * 1024 * 1024) {

            throw new Error(
                `${file.name} 파일이 10MB를 초과했습니다.`
            );
        }
    }


    const formData =
        new FormData();


    files.forEach(file => {

        formData.append(
            "files",
            file
        );
    });


    const csrfToken =
        document.querySelector(
            'meta[name="_csrf"]'
        );

    const csrfHeader =
        document.querySelector(
            'meta[name="_csrf_header"]'
        );


    const headers = {};


    if (csrfToken && csrfHeader) {

        headers[
            csrfHeader.content
            ] = csrfToken.content;
    }


    const response =
        await fetch(
            `/approvals/api/${documentId}/attachments`,
            {
                method: "POST",
                headers,
                body: formData
            }
        );


    if (!response.ok) {

        const message =
            await response.text();

        throw new Error(
            message ||
            "첨부파일 업로드에 실패했습니다."
        );
    }
}